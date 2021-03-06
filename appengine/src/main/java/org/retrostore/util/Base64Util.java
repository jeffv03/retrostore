/*
 *  Copyright 2017, Sascha Häberling
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.retrostore.util;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import javax.xml.bind.DatatypeConverter;

public class Base64Util {
  public static Optional<byte[]> decode(String base64Str) {
    if (Strings.isNullOrEmpty(base64Str)) {
      return Optional.absent();
    }
    return Optional.of(DatatypeConverter.parseBase64Binary(base64Str));
  }
}
