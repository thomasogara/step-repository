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
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The /image-load route exposes a simple API.
 * This API can be accessed by the GET http method only.
 *
 * GET:
 * The request body must contain:
 *   - an 'imageBlobstoreKey' parameter
 * The 'imageBlobstoreKey' parameter is used to identify the image
 * which is to be sent back in the response.
 * The response body will contain the image requested.
 */

/** Servlet that returns the image associated with a BlobKey */
@WebServlet("/image-load")
public class ImageLoadServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String key = getParameter(request, "imageBlobstoreKey", "");
    if ( key.equals("") ) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    BlobKey blobKey = new BlobKey(key);

    // open a connection to blobstore
    BlobstoreService blobstore = BlobstoreServiceFactory.getBlobstoreService();

    blobstore.serve(blobKey, response);
  }

  /**
   * @return the request parameter, or the default value if the parameter
   *         was not specified by the client
   */
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
