#!/bin/bash

#///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#
#  © 2011-2021 Telenav, Inc.
#  Licensed under Apache License, Version 2.0
#
#///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

cd $TDK_HOME/tdk-josm-plugins

PLUGINS=~/Library/JOSM/plugins

mkdir -p $PLUGINS

cp -v geojson/target/tdk-josm-plugins-geojson-*-SNAPSHOT.jar $PLUGINS/telenav-geojson.jar
cp -v graph/target/tdk-josm-plugins-graph-*-SNAPSHOT.jar $PLUGINS/telenav-graph.jar
