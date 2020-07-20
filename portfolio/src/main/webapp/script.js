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
 * @param{Element} entity Element to write to the text field of.
 * @param{String} text The text to write
 * @param{Function} wait_function A function returning an integer value which
 *    is the number of milliseconds for which the site will wait between
 *    printing successive tokens.
 */
const slowFill = async (entity, text, wait_function) => {
  // Indicates if there is a cursor attached to the end of the text field
  let cursor_attached = false;
  
  if (wait_function == null) wait_function = function() {return 50;};
  
  /*
   * The below arrow functions refer to a 'cursor'.
   * This cursor is a reference to the pipe character '|', which
   * is appended to the text field each time a character from the source
   * text is written.
   * The cursor is then removed prior to the writing of the following
   * character from the source text.
   * This achieves a typewriter effect for the user watching the characters
   * slowly fill in the text field.
   */
  /*
   * Remove the cursor from the end of the text field of the
   * entity passed as argument.
   */
  const removeCursor = () => {
    if(cursor_attached){
      entity.innerText = 
          entity.
              innerText.
                  substr(0, entity.innerText.length - 1);
      cursor_attached = !cursor_attached;
    }
  };
  /*
   * Attach a cursor to the end of the text field of the
   * entity passed as argument.
   */
  const attachCursor = () => {
    entity.innerText += cursor_attached ? '' : '|';
    cursor_attached = !cursor_attached;
  };
  
  /*
   * split the text into words using a single space characer
   * ' ' as a seperator, and then split the words into characters
   * using the empty string '' as a delimiter.
   */
  for(let word of text.split(' ')){
    for(let token of word.split('')){
      /*
       * further to the typewriter effect mentioned above, each character
       * is written to the text field, followed by a cursor.
       * wait_function() is then called to induce a delay between the
       * printing of consecutive characters.
      */
      removeCursor();
      entity.innerText += token;
      attachCursor();
      await sleep(wait_function());
    }
    removeCursor();
    /*
     * HTML entity reference was necessary as '&ensp;' cannot
     * otherwise be referenced in a string literal.
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

/**
 * Load comments and add them to the page
 */
const loadComments = async () => {
  const data = await fetch('/comments');
  const comments = await data.json();
  console.log(comments);
  const container = document.getElementById('comments');
  comments.map((comment) => {
    const div = document.createElement('div');
    const paragraph = document.createElement('p');
    container.appendChild(div);
    div.appendChild(paragraph);
    div.id = comment.id;
    paragraph.innerText = `[${comment.timestamp}]: ${comment.text}`;
  });
}

/* Slow fill biography on page load */
const main = () => {
  const window_onload_old = window.onload;
  window.onload = () => {
    if (typeof(window_onload_old) === 'function'){
      window_onload_old();
    }
    slowFillBiography();
    loadComments();
  };
};

main();
