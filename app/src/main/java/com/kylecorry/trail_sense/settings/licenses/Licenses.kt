package com.kylecorry.trail_sense.settings.licenses

object Licenses {

    private const val APACHE_NO_OWNER_SPECIFIED = "[name of copyright owner]"
    private const val APACHE_NO_YEAR_SPECIFIED = "[yyyy]"

    val libraries = listOf(
        Library(
            "Trail Sense",
            "https://github.com/kylecorry31/Trail-Sense"
        ) {
            val contributors = arrayOf("kylecorry31", "qwerty287", "muryno", "Fjuro", "oersen", "ChuckMichael", "weblate", "laralem", "ojppe", "tosinonikute", "fitojb", "comradekingu", "beriain", "bowornsin", "ebraminio", "CloneWith", "zsbetu", "SecularSteve", "shilonit", "Jakarrrg", "jer194", "Kamilkampfwagen-II", "raphaelventura", "SantosSi", "ACABMAN666", "CesPaul", "delthia", "another-sapiens", "AlessandroFrangiamone", "cleveHEX", "khwolf", "BorATICI", "trymbf", "gustavi", "Craftefix", "naoliv", "eagledofc", "crlambda", "Xoronic", "mfrancesconi", "Leopardus4", "comcloudway", "fnogcps", "AsmodeumX", "Portagoras", "StarSkyGeminid", "mrestivill", "s3n-w6i", "TamilNeram", "paulle", "olczcolor", "notramo", "NaserKhoshfetrat", "carter-oswald-dev", "Tyxiel", "tacostea", "Sak94664", "le-jun", "LiJu09", "ingfabby", "hphan9", "KovalevArtem", "AHOHNMYC", "Ricky-Tigg", "imBigo", "SebV60", "yurtpage", "naoritzio", "bgo-eiu", "github-actions[bot]", "huuhaa", "lucasmz-dev", "mellvie", "volodymyr-ahafonov", "asafran", "sguinetti", "tetrdd", "ygorigor", "LuccoJ", "L-P", "LosstarottArt", "NicolaSmaniotto", "Oymate", "Poussinou", "rezaalmanda", "efraletti", "WuerfelDev", "VasilisKos", "Tijs-B", "swltr", "A5468", "Shadowstrike-code", "rherilier", "VectorKappa", "Pastitas", "the7thNightmare", "mgorny", "ARtHryDr", "f4n0", "hrachmnam", "Mohammadshir2004", "nautilusx", "doomed-neko", "kasmide", "tct123", "xax", "jacoii", "zaioti", "jere-a", "btsmartx", "love80312", "Abdulkarim28", "Akamar", "dasrecht", "realgooseman", "helloiamcait", "CanUCMeSharp", "E440QF", "EnderPicture", "Estebastien", "fparri", "Johnny846", "FrameXX", "W113565456", "gerrydoro", "gustavosilveiragss", "IgotDlore", "iRomanyshyn", "teketemdn", "LightFOSS", "LucFreitas", "Luiz-bro", "BatuAtlas", "xmbhasin", "TomasCartman", "Kapral67")
            "${
                mit(
                    "2020-2025",
                    "Kyle Corry"
                )
            }\n\nThe following people contributed code or translations to Trail Sense under the MIT License: ${
                contributors.joinToString(
                    ", "
                )
            }"
        },
        Library(
            "Pictogrammers",
            "https://pictogrammers.com/"
        ) {
            """Pictogrammers Free License
--------------------------

Last Updated: February 1st, 2023

This package is released as free, open-source, and GPL friendly by
the [Pictogrammers](https://pictogrammers.com/). You may use it
for commercial projects, open-source projects, or anything really.

# Icons: Apache 2.0 (https://www.apache.org/licenses/LICENSE-2.0)
Some of the icons are redistributed under the Apache 2.0 license. All other
icons are either redistributed under their respective licenses or are
distributed under the Apache 2.0 license.

# Fonts: Apache 2.0 (https://www.apache.org/licenses/LICENSE-2.0)
All web and desktop fonts are distributed under the Apache 2.0 license. Web
and desktop fonts contain some icons that are redistributed under the Apache
2.0 license. All other icons are either redistributed under their respective
licenses or are distributed under the Apache 2.0 license.

# Code: MIT (https://opensource.org/licenses/MIT)
The MIT license applies to all non-font and non-icon files."""
        },
        Library(
            "Material Design Icons",
            "https://github.com/google/material-design-icons/",
        ) { apache2("year", "Google", "") },
        Library(
            "Material Components for Android",
            "https://github.com/material-components/material-components-android"
        ) {
            apache2(APACHE_NO_YEAR_SPECIFIED, APACHE_NO_OWNER_SPECIFIED, "")
        },
        Library(
            "Android Jetpack",
            "https://github.com/androidx/androidx"
        ) {
            apache2("", "", "")
        },
        Library(
            "RenderScript Intrinsics Replacement Toolkit",
            "https://github.com/android/renderscript-intrinsics-replacement-toolkit"
        ) {
            apache2(APACHE_NO_YEAR_SPECIFIED, APACHE_NO_OWNER_SPECIFIED, "")
        },
        Library(
            "Markwon",
            "https://github.com/noties/Markwon"
        ) {
            apache2("{yyyy}", "{name of copyright owner}", "")
        },
        Library(
            "ZXing",
            "https://github.com/zxing/zxing"
        ) {
            apache2(
                APACHE_NO_YEAR_SPECIFIED,
                APACHE_NO_OWNER_SPECIFIED,
                ""
            )
        },
        Library(
            "CompassView",
            "https://github.com/kix2902/CompassView"
        ) {
            apache2(
                "\${year}",
                "\${owner}",
                "Modifications made by Kyle Corry: Ported to Kotlin, changed styling."
            )
        },
        Library(
            "osgb",
            "https://github.com/dstl/osgb"
        ) {
            apache2(
                APACHE_NO_YEAR_SPECIFIED,
                APACHE_NO_OWNER_SPECIFIED,
                "Modifications made by Kyle Corry: Fixed WGS84 conversion bug"
            )
        },
        Library(
            "kotlinx.coroutines",
            "https://github.com/Kotlin/kotlinx.coroutines"
        ) {
            apache2(
                "2000-2020",
                "JetBrains s.r.o. and Kotlin Programming Language contributors.",
                ""
            )
        },
        Library(
            "kotlin-csv",
            "https://github.com/doyaaaaaken/kotlin-csv"
        ) {
            apache2("2021", "doyaaaaaken", "")
        },
        Library(
            "subsampling-scale-image-view",
            "https://github.com/davemorrissey/subsampling-scale-image-view"
        ) {
            apache2("2020", "davemorrissey", "")
        },
        Library(
            "NPMap Symbol Library",
            "https://github.com/nationalparkservice/symbol-library"
        ) {
            bsd("2013", "Mapbox, LLC")
        },
        Library(
            "MAD Location Manager",
            "https://github.com/maddevsio/mad-location-manager"
        ) {
            mit("2020", "Mad Devs")
        },
        Library(
            "NASA WORLD WIND (ported by Berico-Technologies)",
            "https://github.com/Berico-Technologies/Geo-Coordinate-Conversion-Java"
        ) {
            """NASA WORLD WIND

Copyright © 2004-2005 United States Government as represented by the Administrator of the National Aeronautics and Space Administration. All Rights Reserved. Copyright © 2004-2005 Contributors. All Rights Reserved.

NASA OPEN SOURCE AGREEMENT VERSION 1.3

THIS OPEN SOURCE AGREEMENT ("AGREEMENT") DEFINES THE RIGHTS OF USE, REPRODUCTION, DISTRIBUTION, MODIFICATION AND REDISTRIBUTION OF CERTAIN COMPUTER SOFTWARE ORIGINALLY RELEASED BY THE UNITED STATES GOVERNMENT AS REPRESENTED BY THE GOVERNMENT AGENCY LISTED BELOW ("GOVERNMENT AGENCY"). THE UNITED STATES GOVERNMENT, AS REPRESENTED BY GOVERNMENT AGENCY, IS AN INTENDED THIRD-PARTY BENEFICIARY OF ALL SUBSEQUENT DISTRIBUTIONS OR REDISTRIBUTIONS OF THE SUBJECT SOFTWARE. ANYONE WHO USES, REPRODUCES, DISTRIBUTES, MODIFIES OR REDISTRIBUTES THE SUBJECT SOFTWARE, AS DEFINED HEREIN, OR ANY PART THEREOF, IS, BY THAT ACTION, ACCEPTING IN FULL THE RESPONSIBILITIES AND OBLIGATIONS CONTAINED IN THIS AGREEMENT.

Government Agency: National Aeronautics and Space Administration (NASA) Government Agency Original Software Designation: ARC-15166-1 Government Agency Original Software Title: NASA World Wind User Registration Requested. Please send email with your contact information to Patrick.Hogan@nasa.gov Government Agency Point of Contact for Original Software: Patrick.Hogan@nasa.gov

DEFINITIONS
A. "Contributor" means Government Agency, as the developer of the Original Software, and any entity that makes a Modification. B. "Covered Patents" mean patent claims licensable by a Contributor that are necessarily infringed by the use or sale of its Modification alone or when combined with the Subject Software. C. "Display" means the showing of a copy of the Subject Software, either directly or by means of an image, or any other device. D. "Distribution" means conveyance or transfer of the Subject Software, regardless of means, to another. E. "Larger Work" means computer software that combines Subject Software, or portions thereof, with software separate from the Subject Software that is not governed by the terms of this Agreement. F. "Modification" means any alteration of, including addition to or deletion from, the substance or structure of either the Original Software or Subject Software, and includes derivative works, as that term is defined in the Copyright Statute, 17 USC 101. However, the act of including Subject Software as part of a Larger Work does not in and of itself constitute a Modification. G. "Original Software" means the computer software first released under this Agreement by Government Agency with Government Agency designation ARC-15166-1 and entitled WorldWind, including source code, object code and accompanying documentation, if any. H. "Recipient" means anyone who acquires the Subject Software under this Agreement, including all Contributors. I. "Redistribution" means Distribution of the Subject Software after a Modification has been made. J. "Reproduction" means the making of a counterpart, image or copy of the Subject Software. K. "Sale" means the exchange of the Subject Software for money or equivalent value. L. "Subject Software" means the Original Software, Modifications, or any respective parts thereof. M. "Use" means the application or employment of the Subject Software for any purpose.

GRANT OF RIGHTS
A. Under Non-Patent Rights: Subject to the terms and conditions of this Agreement, each Contributor, with respect to its own contribution to the Subject Software, hereby grants to each Recipient a non-exclusive, world-wide, royalty-free license to engage in the following activities pertaining to the Subject Software:

Use
Distribution
Reproduction
Modification
Redistribution
Display
B. Under Patent Rights: Subject to the terms and conditions of this Agreement, each Contributor, with respect to its own contribution to the Subject Software, hereby grants to each Recipient under Covered Patents a non-exclusive, world-wide, royalty-free license to engage in the following activities pertaining to the Subject Software:

Use
Distribution
Reproduction
Sale
Offer for Sale
C. The rights granted under Paragraph B. also apply to the combination of a Contributor’s Modification and the Subject Software if, at the time the Modification is added by the Contributor, the addition of such Modification causes the combination to be covered by the Covered Patents. It does not apply to any other combinations that include a Modification.

D. The rights granted in Paragraphs A. and B. allow the Recipient to sublicense those same rights. Such sublicense must be under the same terms and conditions of this Agreement.

OBLIGATIONS OF RECIPIENT
A. Distribution or Redistribution of the Subject Software must be made under this Agreement except for additions covered under paragraph 3H.

Whenever a Recipient distributes or redistributes the Subject Software, a copy of this Agreement must be included with each copy of the Subject Software; and
If Recipient distributes or redistributes the Subject Software in any form other than source code, Recipient must also make the source code freely available, and must provide with each copy of the Subject Software information on how to obtain the source code in a reasonable manner on or through a medium customarily used for software exchange.
B. Each Recipient must ensure that the following copyright notice appears prominently in the Subject Software: Copyright (C) 2001 United States Government as represented by the Administrator of the National Aeronautics and Space Administration. All Rights Reserved.

C. Each Contributor must characterize its alteration of the Subject Software as a Modification and must identify itself as the originator of its Modification in a manner that reasonably allows subsequent Recipients to identify the originator of the Modification. In fulfillment of these requirements, Contributor must include a file (e.g., a change log file) that describes the alterations made and the date of the alterations, identifies Contributor as originator of the alterations, and consents to characterization of the alterations as a Modification, for example, by including a statement that the Modification is derived, directly or indirectly, from Original Software provided by Government Agency. Once consent is granted, it may not thereafter be revoked.

D. A Contributor may add its own copyright notice to the Subject Software. Once a copyright notice has been added to the Subject Software, a Recipient may not remove it without the express permission of the Contributor who added the notice.

E. A Recipient may not make any representation in the Subject Software or in any promotional, advertising or other material that may be construed as an endorsement by Government Agency or by any prior Recipient of any product or service provided by Recipient, or that may seek to obtain commercial advantage by the fact of Government Agency's or a prior Recipient’s participation in this Agreement.

F. In an effort to track usage and maintain accurate records of the Subject Software, each Recipient, upon receipt of the Subject Software, is requested to register with Government Agency by visiting the following website: http://opensource.arc.nasa.gov. Recipient’s name and personal information shall be used for statistical purposes only. Once a Recipient makes a Modification available, it is requested that the Recipient inform Government Agency at the web site provided above how to access the Modification.

G. Each Contributor represents that that its Modification is believed to be Contributor’s original creation and does not violate any existing agreements, regulations, statutes or rules, and further that Contributor has sufficient rights to grant the rights conveyed by this Agreement.

H. A Recipient may choose to offer, and to charge a fee for, warranty, support, indemnity and/or liability obligations to one or more other Recipients of the Subject Software. A Recipient may do so, however, only on its own behalf and not on behalf of Government Agency or any other Recipient. Such a Recipient must make it absolutely clear that any such warranty, support, indemnity and/or liability obligation is offered by that Recipient alone. Further, such Recipient agrees to indemnify Government Agency and every other Recipient for any liability incurred by them as a result of warranty, support, indemnity and/or liability offered by such Recipient.

I. A Recipient may create a Larger Work by combining Subject Software with separate software not governed by the terms of this agreement and distribute the Larger Work as a single product. In such case, the Recipient must make sure Subject Software, or portions thereof, included in the Larger Work is subject to this Agreement.

J. Notwithstanding any provisions contained herein, Recipient is hereby put on notice that export of any goods or technical data from the United States may require some form of export license from the U.S. Government. Failure to obtain necessary export licenses may result in criminal liability under U.S. laws. Government Agency neither represents that a license shall not be required nor that, if required, it shall be issued. Nothing granted herein provides any such export license.

DISCLAIMER OF WARRANTIES AND LIABILITIES; WAIVER AND INDEMNIFICATION
A. No Warranty: THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE. THIS AGREEMENT DOES NOT, IN ANY MANNER, CONSTITUTE AN ENDORSEMENT BY GOVERNMENT AGENCY OR ANY PRIOR RECIPIENT OF ANY RESULTS, RESULTING DESIGNS, HARDWARE, SOFTWARE PRODUCTS OR ANY OTHER APPLICATIONS RESULTING FROM USE OF THE SUBJECT SOFTWARE. FURTHER, GOVERNMENT AGENCY DISCLAIMS ALL WARRANTIES AND LIABILITIES REGARDING THIRD-PARTY SOFTWARE, IF PRESENT IN THE ORIGINAL SOFTWARE, AND DISTRIBUTES IT "AS IS."

B. Waiver and Indemnity: RECIPIENT AGREES TO WAIVE ANY AND ALL CLAIMS AGAINST THE UNITED STATES GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT. IF RECIPIENT'S USE OF THE SUBJECT SOFTWARE RESULTS IN ANY LIABILITIES, DEMANDS, DAMAGES, EXPENSES OR LOSSES ARISING FROM SUCH USE, INCLUDING ANY DAMAGES FROM PRODUCTS BASED ON, OR RESULTING FROM, RECIPIENT'S USE OF THE SUBJECT SOFTWARE, RECIPIENT SHALL INDEMNIFY AND HOLD HARMLESS THE UNITED STATES GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT, TO THE EXTENT PERMITTED BY LAW. RECIPIENT'S SOLE REMEDY FOR ANY SUCH MATTER SHALL BE THE IMMEDIATE, UNILATERAL TERMINATION OF THIS AGREEMENT.

GENERAL TERMS
A. Termination: This Agreement and the rights granted hereunder will terminate automatically if a Recipient fails to comply with these terms and conditions, and fails to cure such noncompliance within thirty (30) days of becoming aware of such noncompliance. Upon termination, a Recipient agrees to immediately cease use and distribution of the Subject Software. All sublicenses to the Subject Software properly granted by the breaching Recipient shall survive any such termination of this Agreement.

B. Severability: If any provision of this Agreement is invalid or unenforceable under applicable law, it shall not affect the validity or enforceability of the remainder of the terms of this Agreement.

C. Applicable Law: This Agreement shall be subject to United States federal law only for all purposes, including, but not limited to, determining the validity of this Agreement, the meaning of its provisions and the rights, obligations and remedies of the parties.

D. Entire Understanding: This Agreement constitutes the entire understanding and agreement of the parties relating to release of the Subject Software and may not be superseded, modified or amended except by further written agreement duly executed by the parties.

E. Binding Authority: By accepting and using the Subject Software under this Agreement, a Recipient affirms its authority to bind the Recipient to all terms and conditions of this Agreement and that that Recipient hereby agrees to all terms and conditions herein.

F. Point of Contact: Any Recipient contact with Government Agency is to be directed to the designated representative as follows: Patrick.Hogan@nasa.gov"""
        },
        Library(
            "Natural Earth",
            "https://www.naturalearthdata.com/"
        ) {
            """Made with Natural Earth. Free vector and raster map data @ naturalearthdata.com."""
        },
        Library(
            "MERRA-2",
            "https://disc.gsfc.nasa.gov/datasets/M2SMNXSLV_5.12.4/summary?keywords=statM_2d_slv_Nx"
        ) {
            """Global Modeling and Assimilation Office (GMAO) (2015), MERRA-2 statM_2d_slv_Nx: 2d,Monthly,Aggregated Statistics,Single-Level,Assimilation,Single-Level Diagnostics V5.12.4, Greenbelt, MD, USA, Goddard Earth Sciences Data and Information Services Center (GES DISC), Accessed: 2023-05-22, 10.5067/KVIMOMCUO83U"""
        },
        Library(
            "ERA5",
            "https://cds.climate.copernicus.eu"
        ){
            """Contains modified Copernicus Climate Change Service information 1991 - 2020. Neither the European Commission nor ECMWF is responsible for any use that may be made of the Copernicus information or data it contains.

Hersbach, H., Bell, B., Berrisford, P., Biavati, G., Horányi, A., Muñoz Sabater, J., Nicolas, J., Peubey, C., Radu, R., Rozum, I., Schepers, D., Simmons, A., Soci, C., Dee, D., Thépaut, J-N. (2023): ERA5 monthly averaged data on pressure levels from 1940 to present. Copernicus Climate Change Service (C3S) Climate Data Store (CDS), DOI: 10.24381/cds.6860a573 (Accessed on 17-NOV-2023)"""
        },
        Library(
            "EOT20",
            "https://doi.org/10.17882/79489"
        ) {
            """Hart-Davis Michael, Piccioni Gaia, Dettmering Denise, Schwatke Christian, Passaro Marcello, Seitz Florian (2021). EOT20 - A global Empirical Ocean Tide model from multi-mission satellite altimetry. SEANOE. https://doi.org/10.17882/79489"""
        },
        Library(
            "SIMBAD",
            "https://doi.org/10.1051/aas:2000332"
        ) {
            """This research has made use of the SIMBAD database, operated at CDS, Strasbourg, France. 2000,A&AS,143,9 , "The SIMBAD astronomical database", Wenger et al."""
        },
        Library(
            "ETOPO 2022",
            "https://doi.org/10.25921/fd45-gt74"
        ) {
            """NOAA National Centers for Environmental Information. 2022: ETOPO 2022 15 Arc-Second
Global Relief Model. NOAA National Centers for Environmental Information.
https://doi.org/10.25921/fd45-gt74 . Accessed 2023-05-26.
ETOPO 2022 metadata may be accessed here: ETOPO 2022 metadata landing page"""
        },
        Library(
            "pyTMD",
            "https://github.com/tsutterley/pyTMD",
        ) { mit("2017", "Tyler C Sutterley") },
        Library("OpenCelliD", "https://opencellid.org/") {
            "OpenCelliD Project is licensed under a Creative Commons Attribution-ShareAlike 4.0 International License"
        },
        Library("Mozilla Location Service", "https://archive.org/details/MLS_Full_Cell_Export_Final"){
            "Mozilla Corporation - Public Domain - Final data export, no longer maintained."
        },
        Library("FCC Antenna Registrations", "https://www.fcc.gov/uls/transactions/daily-weekly"){
            "FCC - Public Domain"
        },
        Library(
            "Survival guide",
            ""
        ) {
            """The survival guide is licensed under CC BY-SA 4.0 and was written by Kyle Corry""".trimIndent()
        },
        Library(
            "Elevation colors",
            ""
        ){
            """
                The elevation colors for map layers were designed by various authors.
                
                - USGS: U.S. Geological Survey and R. Langford under CC Attribution-NonCommercial 3.0 unported (2021) https://creativecommons.org/licenses/by-nc/3.0/
                - Muted: Tom Patterson under public domain
                - Viridis, Magma, Plasma, Inferno: Nathaniel J. Smith, Stefan van der Walt, and Eric Firing under CC0 1.0 Universal (http://creativecommons.org/publicdomain/zero/1.0)
            """.trimIndent()
        }
    )

    private fun bsd(year: String, owner: String): String {
        return """BSD License

Copyright© $year, $owner. All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this
  list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

* Neither the name of the Mapbox, LLC. nor the names of its
  contributors may be used to endorse or promote products derived from this
  software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."""
    }

    private fun apache2(year: String, owner: String, modifications: String): String {
        return """
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright $year $owner

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   
   $modifications"""
    }

    fun mit(year: String, owner: String): String {
        return """The MIT License (MIT)

Copyright (c) $year $owner

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE."""
    }

}