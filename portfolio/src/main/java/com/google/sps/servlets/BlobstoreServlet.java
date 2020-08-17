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

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns a link for uploading a form to Blobstore for processing */
@WebServlet("/file-upload")
public class BlobstoreServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Open a connection to blobstore.
    BlobstoreService blobstore = BlobstoreServiceFactory.getBlobstoreService();

    // Create a url which will process a form upload and forward the processed
    // form to the /comments route.
    String url = blobstore.createUploadUrl("/comments");

    // Encode the data as json and send to the client.
    Gson gson = new Gson();
    response.setContentType("application/json");
    response.getWriter().println(gson.toJson(url));
  }
}
