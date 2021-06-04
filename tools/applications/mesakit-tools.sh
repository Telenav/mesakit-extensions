#!/bin/bash

#///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#  © 2020 Telenav - All rights reserved.                                                                               /
#  This software is the confidential and proprietary information of Telenav ("Confidential Information").              /
#  You shall not disclose such Confidential Information and shall use it only in accordance with the                   /
#  terms of the license agreement you entered into with Telenav.                                                       /
#///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

source mesakit-library-functions.sh

FILTER_OUT="grep -y -v --line-buffered"

java -DMESAKIT_LOG=Console -DKIVAKIT_DEBUG="!Debug" -jar $MESAKIT_ASSETS_HOME/docs/$MESAKIT_VERSION/applications/mesakit-tools-$MESAKIT_VERSION.jar $@ 2>&1 | $FILTER_OUT "Illegal reflective access by" | $FILTER_OUT "All illegal access operations will be denied in a future release" | $FILTER_OUT "An illegal reflective access operation has occurred" | $FILTER_OUT "Please consider reporting this to the maintainers of" | $FILTER_OUT "Use --illegal-access=warn to enable warnings of further illegal reflective access operations"
