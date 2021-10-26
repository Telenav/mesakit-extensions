#!/usr/bin/perl

#///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#
#  © 2011-2021 Telenav, Inc.
#  Licensed under Apache License, Version 2.0
#
#///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

use strict;
use warnings FATAL => 'all';

#
# Include build script from cactus-build
#

if (!-d "cactus-build")
{
    system("git clone --branch develop --quiet https://github.com/Telenav/cactus-build.git");
}

require "./cactus-build/.github/scripts/build-include.pl";

#
# Clone repositories and build
#

my ($build_type) = @ARGV;
check_build_type($build_type);
my $github = "https://github.com/Telenav";

clone("$github/kivakit", "dependency");
clone("$github/kivakit-extensions", "dependency");
clone("$github/mesakit", "dependency");
clone("$github/mesakit-extensions", "build");

system("cd ./kivakit/superpom && mvn clean install");

build_kivakit($build_type);

print("Installing GRPC merged");
system("cd ./kivakit-extensions/kivakit-grpc-merged && mvn --batch-mode --no-transfer-progress clean install");
print("Installing Protostuff merged");
system("cd ./kivakit-extensions/kivakit-protostuff-merged && mvn --batch-mode --no-transfer-progress clean install");
print("Installing Prometheus merged");
system("cd ./kivakit-extensions/kivakit-prometheus-merged && mvn --batch-mode --no-transfer-progress clean install");

build_kivakit_extensions($build_type);
build_mesakit($build_type);
build_mesakit_extensions($build_type);
