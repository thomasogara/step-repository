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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns a programmable number of comments */
@WebServlet("/comments")
public class DataServlet extends HttpServlet {
  private final static int ALL_COMMENTS = -1;


  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String parameterMaxComments = getParameter(request, "maxComments", "");
    int maxComments = 0;

    try{
      maxComments = Integer.parseInt(parameterMaxComments);
    } catch (NumberFormatException ex) {
      // If maxComments parameter is excluded or malformed, return all comments
      maxComments = DataServlet.ALL_COMMENTS;
    }

    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    FetchOptions fetchOptions = FetchOptions.Builder.withLimit(maxComments);

    List<Comment> comments = new ArrayList<>();
    for ( Entity entity : results.asIterable(fetchOptions) ) {
      long id = entity.getKey().getId();
      String title = (String) entity.getProperty("title");
      String text = (String) entity.getProperty("text");
      long timestamp = (long) entity.getProperty("timestamp");

      Comment comment = new Comment(id, title, text, timestamp);
      comments.add(comment);
    }

    Gson gson = new Gson();

    response.setContentType("application/json");
    response.getWriter().println(gson.toJson(comments));
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
    String title = getParameter(request, "title", "");
    String text = getParameter(request, "text", "");
    long timestamp = System.currentTimeMillis();
    
    /* 
     * \\s represents any whitespace character
     * + is a quantifier. it translates to 'one or more'
     * the pattern therefore matches 'one or more whitespace characters'
     */
    String WHITESPACE_REGEX = "\\s+";
    
    /* remove all whitespace from the text String */
    String commentTextWhitespaceRemoved = text.replaceAll(WHITESPACE_REGEX, "");
    
    /* if the commentText String, with all whitespace removed, is empty, then the comment is rejected */
    if (commentTextWhitespaceRemoved.equals("")) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("title", title);
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
}
