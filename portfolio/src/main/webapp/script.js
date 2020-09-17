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
};

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
};

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
  /*
   * The /comments route exposes a simple API.
   * This API is available only using the GET http method.
   * Requests sent to this API must have a body which contains:
   *   - nothing
   *   - A single parameter, 'maxComments'
   * If the request body is non-empty, then maxComments is used to limit
   * the number of results returned by the request.
   * The response body will be encoded as json.
   * The response body will contain a single top-level array, whose
   * elements will all be Comment objects.
   * Comment objects have three members:
   *   id: the id of the comment in the server's datastore
   *   title: the comment title
   *   text: the comment text
   */
  const data = await fetch(`/comments?maxComments=${maxComments}`);
  /* Parse the JSON response, and store the resulting array */
  const comments = await data.json();

  const container = document.getElementById('comments');
  // Clear the contents of the div containing the comments.
  // This prevents duplication of comments on the front-end.
  container.innerHTML = '';

  /*
   * Process each comment, and add it to the DOM.
   * Comments are displayed on screen as a div, which has 5 direct children.
   * The children of the comment div are:
   *   - A paragraph element, displaying the comment's text.
   *   - A h3 element, displaying the comment's title.
   *   - An img element, displaying the comment's iamge if it exists.
   *   - A span element, displaying the comment's timestamp.
   *   - A button element, used to allow comment deletion.
   */
  comments.map((comment) => {
    const div = document.createElement('div');
    const paragraph = document.createElement('p');
    const title = document.createElement('h3');
    const commentImage = document.createElement('img');
    const timestamp = document.createElement('span');
    const deleteButton = document.createElement('button');
    const trashImage = document.createElement('img');

    container.appendChild(div);
    div.appendChild(title);
    div.appendChild(commentImage);
    div.appendChild(paragraph);
    div.appendChild(timestamp);
    div.appendChild(deleteButton);
    deleteButton.appendChild(trashImage);

    div.classList.add('position-relative');
    commentImage.classList.add('mw-100');
    /*
     * comment-delete-button is a custom css class in style.css
     * It is the only aspect of the styling that is not done with bootstrap.
     * The class places the delete button in the bottom right of the comment.
     */
    deleteButton.classList.add('comment-delete-button');

    div.id = comment.id;
    title.innerText = comment.title;
    loadCommentImage(comment.imageBlobstoreKey).then((url) => {
      commentImage.src = url;
    });
    paragraph.innerText = comment.text;
    timestamp.innerText = new Date(comment.timestamp).toLocaleString();
    trashImage.src = '/images/trash.png';
    /*
     * Set the function handling the onclick event of the delete button.
     * The function will not be called until the user decides to delete a comment.
     */
    deleteButton.onclick = async () => {
      // the client must wait for confirmation of comment deletion
      // before proceeding to refresh comments, otherwise client will
      // fall out of sync with the server
      const response = await deleteComment(comment.id, comment.imageBlobstoreKey);
      if ( !response.ok ) {
        alert('Your comment could not be deleted. Please refresh the page and try again.');
      } else {
        refreshComments();
      }
    };
  });
};

/**
 * @return {boolean} Whether the user is loggged in
 */
const checkLoginStatus = async () => {
  let loggedIn = false;
  const loginServiceUnavailable =
      `Sorry, the login service is down! :(
       Please refresh the page if you would like to log in.`;
  const loginServiceMalfunction =
      `Sorry, the login service is experiencing trouble! :(
      Please refresh the page if you would like to log in.`
  fetch('/login-status')
      .catch(error => alert(loginServiceUnavailable))
      .then(response => response.json())
          .catch(error => alert(loginServiceMalfunction))
          .then(responseBody => loggedIn = resonseBody.loggedIn);

  return loggedIn;
};

/**
 * Dispaly a login form to the user if they are not logged in.
 */
const displayLoginForm = async () => {
  const loggedIn = await checkLoginStatus();

  if (loggedIn) {
    // If the user is logged in, hide the login prompt
    const container = document.getElementById('login-container');
    container.style['display'] = 'none';
  } else {
    // If the user is not logged in, fetch a login link for the user
    // This login link will send the user to Google's login page,
    // and will then redirect back to the home page.
    const login_response = await fetch('/login');
    const href = await login_response.json();
    const link = document.getElementById('login-link');

    link.href = href;
  }
};

/**
 * Display a comment form to the user if they are logged in.
 */
const displayCommentForm = async () => {
  const loggedIn = await checkLoginStatus();
  const commentForm = document.getElementById('comment-form');

  if (loggedIn) {
    // If the user is logged in,fetch an upload URL from
    // BlobStore to facilitate form submission.
    loadFormAction();

    // set the form to validate login status before submission
    commentForm.onsubmit = commentFormValidator;
  } else {
    // If the user is not logged in, hide the comment form
    commentForm.style['display'] = 'none';
  }
};

/**
 * Load the URL for uploading the comment form.
 */
const loadFormAction = async () => {
  /*
   * The /file-upload route exposes a simple API.
   * This API is available only using the GET http method.
   * Requests sent to this API must have an empty body.
   * The response body will be encoded as JSON.
   * The response body will contain a single top-level string.
   * This string points to the upload route created by the BlobStore
   * API.
   */
  const response = await fetch('/file-upload');
  const url = await response.json();
  const formElement = document.getElementById('comment-form');
  /* Set the comment form to upload to the route provided by BlobStore */
  formElement.action = url;
};

/**
 * Returns a URL pointing to a comment's image.
 * @param {string} imageBlobstoreKey The blobstore key of the image to be
 * loaded.
 */
const loadCommentImage = async (imageBlobstoreKey) => {
  // request the image from the image load servlet.
  const response = await fetch(
      `/image-load?imageBlobstoreKey=${imageBlobstoreKey}`
  );
  // extract the blob from the response. this blob will be the comment image.
  const blob = await response.blob();
  // store the blob locally in the browser's storage, and create a URL pointing
  // to this local resource.
  const imageURL = window.URL.createObjectURL(blob);
  return imageURL;
};

/**
 * @return {boolean} Whether the comment form meets the necessary conditions for submission
 */
const commentFormValidator = async () => {
  const loggedIn = await checkLoginStatus();
  if ( !loggedIn ) {
    alert('You must login before posting a comment');
    // If the user is not logged in, return false, preventing form submission
    return false;
  } else {
    // If the user is logged in, return true, allowing form submission
    return true;
  }
};

/**
 * Send a POST request to the server, to delete a comment.
 * @param {number|string} id id of the comment to delete
 * @param {string} imageBlobstoreKey blobstore key of the comment image
 */
const deleteComment = async (id, imageBlobstoreKey) => {
  const data = {'id': id, 'imageBlobstoreKey': imageBlobstoreKey};
  const request = {
    method: 'POST',
    body: JSON.stringify(data)
  };
  // comment deletion cannot be a background task. the response
  // must be awaited so as to ensure synchronisation between client and server
  const response = await fetch('/delete-comment', request);
  return response;
};

/**
 * Refresh the comments on the page
 * The select element on the page is polled for the number of comments to load.
 */
const refreshComments = async () => {
  const selectElement = document.getElementById('maxComments');
  // the onchange() attribute of the select element is re-used
  // to allow for the comments to be refreshed from any context
  // in the code base
  selectElement.onchange();
};

/**
 * Handle the change of state of the <select> element
 * on the page.
 * @param{Number} value The current value of the select element
 */
const selectionChangeHandler = async (value) => {
  // update the value of maxComments in the session storage,
  // and reload the comments according to the updated value
  sessionStorage.setItem('maxComments', value);
  loadComments(value);
};

/**
 * Set the value of the #maxComments element on the page
 * @param{Number} value The value to be assigned
 */
const setMaxComments = (value) => {
  const maxCommentsElement = document.getElementById('maxComments');
  const selectedIndex = maxCommentsElement.selectedIndex;
  // constructing the optionValues[] array in this way is required as
  // maxCommentsElement.options is an HTMLCollection object, and does not
  // have a .indexOf() function
  const optionValues = [];
  for (let option of maxCommentsElement.options) {
    optionValues.push(option.value);
  }
  let index = optionValues.indexOf(value);
  // .indexOf() will return -1 if the item is not present in the array
  const notFound = -1;
  // if the passed value is not a valid option (not found in options array), set the value to 5
  if (index === notFound) {
    // option with value 5 is stored at index 0
    index = 0;
  }
  maxCommentsElement.selectedIndex = index;
  sessionStorage.setItem('maxComments', maxCommentsElement[index]);
};

window.onload = async () => {
  // display login form, if user is not logged in
  // else, hide the login form
  displayLoginForm();
  //display comment form, if the user is logged in,
  // else, hide the comment form
  displayCommentForm();

  slowFillBiography();
  // retrieve the 'maxComments' value from sessionStorage if a value
  // has been set. if not, set maxComments to 5
  const maxComments = sessionStorage.getItem('maxComments') || '5';
  // set the value of the select element to the value
  // supplied by session storage
  setMaxComments(maxComments);
  selectionChangeHandler(maxComments);
};
