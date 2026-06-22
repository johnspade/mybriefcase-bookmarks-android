{ pkgs }:

let
  androidSdk = (pkgs.androidenv.composeAndroidPackages {
    platformVersions = [ "35" "36" ];
    buildToolsVersions = [ "35.0.1" "36.0.0" ];
    includeEmulator = false;
    includeNDK = false;
    includeSystemImages = false;
  }).androidsdk;
in

pkgs.dockerTools.buildLayeredImage {
  name = "roborazzi-screenshot-recorder";
  tag = "latest";
  contents = [
    pkgs.bashInteractive
    pkgs.coreutils
    pkgs.findutils
    pkgs.gnugrep
    pkgs.gnused
    pkgs.gawk
    pkgs.which
    pkgs.git
    pkgs.cacert
    pkgs.jdk21
    pkgs.rustToolchain
    pkgs.gcc
    androidSdk
  ];
  extraCommands = ''
    mkdir -p lib64
    ln -s ${pkgs.glibc}/lib/ld-linux-x86-64.so.2 lib64/ld-linux-x86-64.so.2
    mkdir -p usr/bin
    ln -s ${pkgs.coreutils}/bin/env usr/bin/env
    mkdir -p etc/ssl/certs
    ln -s ${pkgs.cacert}/etc/ssl/certs/ca-bundle.crt etc/ssl/certs/ca-certificates.crt
    mkdir -p etc tmp root
    echo "root:x:0:0:root:/root:/bin/bash" > etc/passwd
    echo "root:x:0:" > etc/group
  '';
  config = {
    Labels = {
      "org.opencontainers.image.source" = "https://github.com/johnspade/mybriefcase-bookmarks-android";
    };
    Env = [
      "JAVA_HOME=${pkgs.jdk21}"
      "ANDROID_HOME=${androidSdk}/libexec/android-sdk"
      "SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt"
      "LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath [ pkgs.stdenv.cc.cc.lib ]}"
      "PATH=${pkgs.lib.makeBinPath [
        pkgs.bashInteractive pkgs.coreutils pkgs.findutils
        pkgs.gnugrep pkgs.gnused pkgs.gawk pkgs.which pkgs.git
        pkgs.jdk21 pkgs.rustToolchain pkgs.gcc
      ]}:${androidSdk}/libexec/android-sdk/cmdline-tools/latest/bin"
    ];
    WorkingDir = "/project";
  };
}
