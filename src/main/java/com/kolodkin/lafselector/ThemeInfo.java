/*
 * Copyright 2019 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kolodkin.lafselector;

import java.io.File;

public class ThemeInfo {

    final String name;
    final String resourceName;
    final boolean dark;
    final String license;
    final String licenseFile;
    final String sourceCodeUrl;
    final String sourceCodePath;
    final File themeFile;
    final String �lassName;

    ThemeInfo(String name, String resourceName, boolean dark,
            String license, String licenseFile,
            String sourceCodeUrl, String sourceCodePath,
            File themeFile, String lafClassName) {
        this.name = name;
        this.resourceName = resourceName;
        this.dark = dark;
        this.license = license;
        this.licenseFile = licenseFile;
        this.sourceCodeUrl = sourceCodeUrl;
        this.sourceCodePath = sourceCodePath;
        this.themeFile = themeFile;
        this.�lassName = lafClassName;
    }
    
    public String getClassName() {
        return �lassName;
    }
}
