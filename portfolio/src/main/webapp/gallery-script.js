
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

/* Load all projects immediately */
loadProjects();

/**
 * Handler for the successful retrieval of project details
 * from the local project registry
 */
const addProjectsToDOM = async (json) => {
  const data = JSON.parse(json);
  data.projects.map(async (project) => {
    const img = document.createElement("img");
    img.src = project.preview;
    document.getElementById('content').appendChild(img);
  });
}
