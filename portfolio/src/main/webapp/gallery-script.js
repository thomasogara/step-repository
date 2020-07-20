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
 * from the local project registry
 */
const addProjectsToDOM = async (json) => {
  /*
    json will always contain a top-level object which has a single member.
    this member will be an array of objects. these objects will have three properties:
    - title : a string containing the title of the project
    - URL : a string which contains a URL linking to the source code of
        the project
    - preview : a string which contains a URL linking to the preview image
        for this project
    - description : a string which contains a short description of this project
    - blog_link : a string which contains a URL linking to a blog post about
        the project
  */
  console.log(json);
  const data = JSON.parse(json);
  /*
    process each project contained within the json object,
    and append an img element with that project's preview
    image to the 'content' div of gallery.html
  */
  data.projects.map(async (project) => {
    const content = document.getElementById('content');
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
