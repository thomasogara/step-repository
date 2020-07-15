// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * A wrapper around slowFill(), with the specific purpose of slowly
 * filling out the biography section of the website.
 */
const slowFillBiography = async () => {
  const biography_text = 
      'Hi! My name\'s Thomas, and I\'m just about to start my third year of ' +
      'studies in University College Dublin. I\'m currently a STEP intern ' +
      'at Google, and this portfolio aims to showcase my contributions ' + 
      'during my time here.';
  const output_element = document.getElementById('biography');
  const wait_function = () => (Math.floor(Math.random() * 50) + 50);
  slowFill(output_element, biography_text, wait_function);
}

/**
 * Fill the text field of a given entity with a given string of text, character
 * by character, at a programmable rate.
 * @param entity The entity to write to the text field of.
 * @param text The text to write to the text field of the entity.
 * @param wait_function A function returning an integer value which is the
 *    number of milliseconds for which the site will wait between printing
 *    successive tokens.
 */
const slowFill = async (entity, text, wait_function) => {
  /*
    Boolean value indicating if there is a cursor attached to the
    end of the text field being written to.
  */
  let cursor_attached = false;

  /* 
    wait_function must be non-null.
    all null inputs are normalised to a known default.
   */
  if (wait_function == null) wait_function = function() {0};

  const removeCursor = () => {
    if(cursor_attached){
      entity.innerText = 
          entity.
              innerText.
                  substr(0, entity.innerText.length - 1);
      cursor_attached = !cursor_attached;
    }
  };
  const attachCursor = () => {
    entity.innerText += cursor_attached ? '' : '|';
    cursor_attached = !cursor_attached;
  };
  
  for(let word of text.split(' ')){
    for(let token of word.split('')){
      removeCursor();
      entity.innerText += token;
      attachCursor();
      await sleep(wait_function());
    }
    removeCursor();
    /*
      HTML entity reference was necessary as '&ensp;' cannot
      otherwise be referenced in a string literal.
    */
    entity.innerHTML += '&ensp;';
  }
}

/**
 * Utility function to allow for any async function to halt
 * execution for a given number of milliseconds.
 * @param time_ms The number of milliseconds for which the calling function
 *    should cease execution.
 */
const sleep = async (time_ms) => (
  new Promise((resolve, reject) => setTimeout(resolve, time_ms))
);

/* Slow fill biography on page load */
const main = () => {
  const window_onload_old = window.onload;
  window.onload = () => {
    window_onload_old();
    slowFillBiography();
  };
};

main();
