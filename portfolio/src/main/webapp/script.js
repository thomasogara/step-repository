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
      `Hi! My name\'s Thomas, and I\'m just about to start my third year of
      studies in University College Dublin. I\'m currently a STEP intern
      at Google, and this portfolio aims to showcase my contributions
      during my time here.`;
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
  
  if (wait_function == null) wait_function = function() {50};
  
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
 * Fetch a given number of comments and add them to the DOM.
 * @param{Number} maxComments The maximum number of comments to fetch
 *    from the server.
 */
const loadComments = async (maxComments) => {
  const data = await fetch(`/comments?maxComments=${maxComments}`);
  const comments = await data.json();
  const container = document.getElementById('comments');
  // clear the contents of the div containing the comments.
  // this prevents duplication of comments on the front-end.
  container.innerHTML = '';

  // process each comment, and add it to the DOM.
  // currently, comments are simply paragraphs embedded
  // in a div container
  comments.map((comment) => {
    const div = document.createElement('div');
    const paragraph = document.createElement('p');
    container.appendChild(div);
    div.appendChild(paragraph);
    div.id = comment.id;
    paragraph.innerText = `[${comment.timestamp}]: ${comment.text}`;
  });
}

/**
 * Handle the change of state of the <select> element
 * on the page.
 * @param{Number} value The current value of the select element
 */
const selectionChangeHandler = async (value) => {
  // #hiddenMaxComments is a hidden input entry in the form on the home page.
  // it is a shadow of the select element, and is submitted along with the form.
  // it will be used in the functionality of a later update.
  const formMaxCommentsElement = document.getElementById('hiddenMaxComments');
  formMaxCommentsElement.value = value;

  // once #hiddenMaxComments has been updated, reload the comments according to
  // the updated value of the select element
  loadComments(value);
}

/**
 * Get a specific GET parameter from the query string
 * @param{String} name The name of the parameter
 */
const getParameter = (name) => {
  const queryString = window.location.search;
  const urlParams = new URLSearchParams(queryString);
  return urlParams.get(name);
}

/**
 * Set the value of the #maxComments element on the page
 * @param{Number} value The value to be assigned
 */
const setMaxComments = (value) => {
  const maxCommentsElement = document.getElementById('maxComments');
  const selectedIndex = maxCommentsElement.selectedIndex;
  const optionValues = [];
  // constructing the optionValues[] array in this way is required as
  // maxCommentsElement.options is an HTMLCollection object, and does not
  // have a .indexOf() function
  for (let option of maxCommentsElement.options) {
    optionValues.push(option.value);
  }
  const index = optionValues.indexOf(value);
  // while -1 is a valid value for the value of the select element, 5 is the
  // intended default. allowing all unknown values to be normalised to -1 would
  // result in all comments being displayed for all unknown values. this also
  // would apply to cases where no maxComments parameter is received.
  if (index === -1) {
    index = 5;
  }
  maxCommentsElement.selectedIndex = index;
}

window.onload = async () => {
  slowFillBiography();
  // retrieve the 'maxComments' GET parameter from the URL string
  // and use it to only load the requested number of comments on page load.
  const maxComments = getParameter('maxComments') || '5';
  // set the value of the select element to the value supplied by the
  // GET parameter.
  setMaxComments(maxComments);
  selectionChangeHandler(maxComments);
}
