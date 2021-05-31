////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// © 2011-2021 Telenav, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.telenav.mesakit.graph.geocoding.reverse.matching;

import com.telenav.mesakit.map.geography.project.KivaKitMapGeographyUnitTest;
import com.telenav.mesakit.map.region.locale.MapLocale;
import com.telenav.mesakit.map.road.model.RoadName;
import com.telenav.mesakit.map.road.name.standardizer.RoadNameStandardizer;
import com.telenav.mesakit.map.road.name.standardizer.RoadNameStandardizer.Mode;

/**
 * This class implements
 *
 * @author Junwei
 * @version 1.0.0 2012-7-6
 */
@Ignore
public class RoadNameStandardizerTest extends KivaKitMapGeographyUnitTest
{
    // @Test
    public void testStandardizeBR()
    {
        // check("Av Auro Soares de Moura Andrade", Locale.PORTUGUESE_BRAZIL,
        // "AVENIDA AURO SOARES DE MOURA ANDRADE");
        // check("R Salvador Felisoni", Locale.PORTUGUESE_BRAZIL, "RUA SALVADOR FELISONI");
        check("Estr do Campo Limpo", MapLocale.PORTUGUESE_BRAZIL.get(), "ESTR DO CAMPO LIMPO");
        // check("R Elizabeth de Souza Campos", Locale.PORTUGUESE_BRAZIL, "RUA ELIZABETH DE SOUZA
        // CAMPOS");
    }

    // @Test
    public void testStandardizeMX()
    {
        check("ANILLO PERIFERICO", MapLocale.SPANISH_MEXICO.get(), "ANILLO PERIFERICO");
        check("CALLE DE CHIMALHUACAN", MapLocale.SPANISH_MEXICO.get(), "CALLE DE CHIMALHUACAN");
    }

    @Test
    public void testStandardizeUS()
    {
        check("Waring Street", MapLocale.ENGLISH_UNITED_STATES.get(), "WARING ST");
        check("Bluff", MapLocale.ENGLISH_UNITED_STATES.get(), "BLUFF");

        // check("US-129", Locale.ENGLISH_UNITED_STATES, "129");
        check("N PETER ST/OLYMPIC DR", MapLocale.ENGLISH_UNITED_STATES.get(), "N PETER ST/OLYMPIC DR");

        // check("Six Flags Parkway", Locale.ENGLISH_UNITED_STATES, "SIX FLAGS PKY");
        // check("Riverside Parkway", Locale.ENGLISH_UNITED_STATES, "RIVERSIDE PKY");

        check("GA-400", MapLocale.ENGLISH_UNITED_STATES.get(), "400");
        check("I-85/EXIT 87", MapLocale.ENGLISH_UNITED_STATES.get(), "85/EXIT 87");

        // check("I-16", Locale.ENGLISH_UNITED_STATES, "16");
        check("GA-358/EXIT 27", MapLocale.ENGLISH_UNITED_STATES.get(), "358/EXIT 27");

        check("MEMORIAL DR", MapLocale.ENGLISH_UNITED_STATES.get(), "MEMORIAL DR");
        check("GA-154/TRINITY AV", MapLocale.ENGLISH_UNITED_STATES.get(), "154/TRINITY AVE");

        check("Rock Quarry Rd", MapLocale.ENGLISH_UNITED_STATES.get(), "ROCK QUARRY RD");
        // check("North Park Trail", Locale.ENGLISH_UNITED_STATES, "N PARK TRAIL");
    }

    private void check(final String streetName, final MapLocale locale, final String expectedStreetName)
    {
        ensureEqual(RoadName.forName(expectedStreetName),
                RoadNameStandardizer.get(locale, Mode.G2_STANDARDIZATION).standardize(RoadName.forName(streetName)));
    }
}
