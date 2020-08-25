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

package com.google.sps.servlets;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreFailureException;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

/** Servlet that deletes a comment whose id is passed to it. */
@WebServlet("/delete-comment")
public class DeletionServlet extends HttpServlet {

  /**
   * POST handler for the /delete-comment route
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
    /* 
     * Get the body of the request.
     * deletionRequestJSON must be a json encoded DeletionRequestBody, with shape similar to below:
     * {
     *   "id": COMMENT_ID
     * }
     */
    String deletionRequestJSON = IOUtils.toString(request.getReader());

    // Parse the json request body, and create a DeletionRequestBody from its contents.
    Gson gson = new Gson();
    DeletionRequestBody body = null;
    try {
      body = gson.fromJson(deletionRequestJSON, DeletionRequestBody.class);
    } catch (JsonSyntaxException ex) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    // Initialise a connection to DataStore.
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    // Each key has a unique id associated with it.
    // This id can be used to reconstruct the key using .createKey()
    Key key = KeyFactory.createKey("Comment", body.getId());

    try {
      datastore.delete(key);
    } catch (DatastoreFailureException ex) {
      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    // Initialise a connection to Blobstore
    BlobstoreService blobstore = BlobstoreServiceFactory.getBlobstoreService();

    // Create a BlobKey from the provided BlobKey String
    // In case of an invalid or non-existent BlobKey, the servlet will quit.
    // This is expected, as some comments don't have images but should still
    // be viable for deletion.
    BlobKey blobKey = new BlobKey(body.getImageBlobstoreKey());

    try {
      blobstore.delete(blobKey);
    } catch (BlobstoreFailureException exception) {
      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }
  }

  /**
   * @return the request parameter, or the default value if the parameter
   *         was not specified by the client.
   */
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  /**
    * The body of a request sent to the deletion servlet.
    */
  private class DeletionRequestBody {
    // The only parameter in the body is the id of the comment to be deleted.
    private long id;
    private String imageBlobstoreKey;

    public long getId() {
      return this.id;
    }

    public String getImageBlobstoreKey() {
      return this.imageBlobstoreKey;
    }
  }
}
