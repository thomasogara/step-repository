/**
 * Load project details from local registry of projects (projects.json)
 */
const loadProjects = async() => {
  const response = await fetch('./projects.json');
  const json = await response.text();
  addProjectsToDOM(json);
}

/**
 * Handler for the successful retrieval of project details
 * from the local project registry.
 * @param{Object} json must be an object which has a single member.
    this member must be an array of objects. these objects must have five properties:
    - title{String} : the title of the project
    - URL{String} : URL linking to the source code of the project
    - preview{String} : URL linking to the preview image for this project
    - description{String} : short description of this project
    - blog_link{String} : URL linking to a blog post about the project
 */
const addProjectsToDOM = async (json) => {
  console.log(json);
  const data = JSON.parse(json);
  /*
    process each project contained within the json object,
    and append an img element with that project's preview
    image to the 'content' div of gallery.html
  */
  json.projects.map(async (project) => {
    const content = document.getElementById('projects');
    const link = document.createElement('a');
    const header = document.createElement('h2');
    const details = document.createElement('p');
    const div = document.createElement('div');
    const img = document.createElement('img');
    content.appendChild(div);
    div.appendChild(link);
    div.appendChild(img);
    link.appendChild(header);
    div.appendChild(details);
    link.href = project.URL;
    header.innerText = project.title;
    img.src = project.preview;
    details.innerText = project.description;
  });
}

/* Load all projects immediately */
const main = () => {
  const window_onload_old = window.onload;
  window.onload = () => {
    if (typeof(window_onload_old) === 'function'){
      window_onload_old();
    }
    loadProjects();
  };
};

main();
