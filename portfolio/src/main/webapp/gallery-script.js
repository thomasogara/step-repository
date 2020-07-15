/**
 * Load project details from local registry of projects (projects.json)
 */
const loadProjects = async() => {
  const request = new XMLHttpRequest();
  request.open("GET", "projects.json", true);
  request.onload = () => addProjectsToDOM(request.responseText);
  request.onerror = () => console.error(request.statusText);
  request.send(null);
}

/**
 * Handler for the successful retrieval of project details
 * from the local project registry
 */
const addProjectsToDOM = async (json) => {
  /*
    json will always contain a top-level object which has a single member.
    this member will be an array of objects. these objects will have three properties:
    - title : a string containing the title of the project
    - URL : a string which contains a URL linking to the blog post about
        the project
    - preview : a string which contains a URL linking to the preview image
        for this project
  */
  const data = JSON.parse(json);
  data.projects.map(async (project) => {
    const img = document.createElement("img");
    img.src = project.preview;
    document.getElementById('content').appendChild(img);
  });
}

/* Load all projects immediately */
const main = () => {
  const window_onload_old = window.onload;
  window.onload = () => {
    window_onload_old();
    loadProjects();
  };
};

main();
