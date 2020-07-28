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

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.MalformedURLException;
import java.util.Map;
import java.net.URL;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * The /comments route exposes a simple API
 * This API can be accessed by the GET and POST http methods only.
 * The API allows a client to either:
 *   - Get a programmable number of comments from the server
 *   - Post a single comment to the server and store it in the datastore
 *
 * GET:
 * The request body does not have any mandatory parameters.
 * The request body may optionally contain:
 *   - A single parameter, 'maxComments'
 * If the maxComments parameter is included, then maxComments is used to limit
 * the number of results returned by the request.
 * The response body will be encoded as json.
 * The response body will contain a single top-level array, whose
 * elements will all be Comment objects.
 * Comment objects have three members:
 *   id: the id of the comment in the server's datastore
 *   title: the comment title
 *   text: the comment text
 *
 * POST:
 * The request body must contain:
 *   - A 'text' parameter
 * The request body may optionally contain:
 *   - A 'title' parameter
 *   - A 'imageURL' parameter
 * The response will be a redirect to the homepage of the site.
 */

/** Servlet that returns a programmable number of comments */
@WebServlet("/comments")
public class DataServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    long maxComments = Long.parseLong(request.getParameter("maxComments"));

    // a value of -1 indicates that ALL comments are wanted
    if (maxComments == -1) maxComments = Long.MAX_VALUE;

    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    List<Comment> comments = new ArrayList<>();
    for ( Entity entity : results.asIterable() ) {
      if (comments.size() >= maxComments ) {
        break;
      }
      long id = entity.getKey().getId();
      String title = (String) entity.getProperty("title");
      String imageURL = (String) entity.getProperty("imageURL");
      String text = (String) entity.getProperty("text");
      long timestamp = (long) entity.getProperty("timestamp");

      Comment comment = new Comment(id, title, imageURL, text, timestamp);
      comments.add(comment);
    }

    Gson gson = new Gson();

    response.setContentType("application/json");
    response.getWriter().println(gson.toJson(comments));
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
    String title = getParameter(request, "title", "");
    String imageURL = getUploadedFileUrl(request, "imageURL");
    String text = getParameter(request, "text", "");
    long timestamp = System.currentTimeMillis();
    
    /* 
     * \\s represents any whitespace character
     * + is a quantifier. it translates to 'one or more'
     * the pattern therefore matches 'one or more whitespace characters'
     */
    String WHITESPACE_REGEX = "\\s+";
    
    /* remove all whitespace from the commentText String */
    String commentTextWhitespaceRemoved = text.replaceAll(WHITESPACE_REGEX, "");
    
    /* if the commentText String, with all whitespace removed, is empty, then the comment is rejected */
    if (commentTextWhitespaceRemoved.equals("")) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("title", title);
    commentEntity.setProperty("imageURL", imageURL);
    commentEntity.setProperty("text", text);
    commentEntity.setProperty("timestamp", timestamp);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);

    response.sendRedirect("/index.html");
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

  /** Returns a URL that points to the uploaded file, or null if the user didn't upload a file. */
  private String getUploadedFileUrl(HttpServletRequest request, String formInputElementName) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get(formInputElementName);

    // User submitted form without selecting a file, so we can't get a URL. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so we can't get a URL. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }

    // Use ImagesService to get a URL that points to the uploaded file.
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);

    // To support running in Google Cloud Shell with AppEngine's dev server, we must use the relative
    // path to the image, rather than the path returned by imagesService which contains a host.
    try {
      URL url = new URL(imagesService.getServingUrl(options));
      return url.getPath();
    } catch (MalformedURLException e) {
      return imagesService.getServingUrl(options);
    }
  }
}
