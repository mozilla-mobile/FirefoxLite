/*
Copyright 2013-2015 David Morrissey

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.davemorrissey.labs.subscaleview.decoder;

/**
 * Interface for decoder (and region decoder) factories.
 * @param <T> the class of decoder that will be produced.
 */
public interface DecoderFactory<T> {
  /**
   * Produce a new instance of a decoder with type {@link T}.
   * @return a new instance of your decoder.
   */
  T make() throws IllegalAccessException, InstantiationException;
}
