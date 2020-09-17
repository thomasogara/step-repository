/**
 * Load project details from local registry of projects (projects.json)
 */
const loadProjects = async() => {
  const response = await fetch('./projects.json');
  const json = await response.json();
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
  /*
    process each project contained within the json object,
    and append an img element with that project's preview
    image to the 'content' div of gallery.html
  */
  json.projects.map(async (project) => {
    const projectsContainer = document.getElementById('projects');
    const sourceCodeLink = document.createElement('a');
    const blogLink = document.createElement('a');
    const title = document.createElement('h2');
    const details = document.createElement('p');
    const projectDiv = document.createElement('div');
    const img = document.createElement('img');

    /* Add a div to the DOM to store the information on this project */
    projectsContainer.appendChild(projectDiv);

    /* Add the project title at the top of the project div */
    /* The title is wrapped in a link, linking to the blog post for the project */
    blogLink.appendChild(header);
    projectDiv.appendChild(blogLink);

    /* Add the project image below the project title */
    /* The image is wrapped in a link, linking to the source code for the project */
    sourceCodeLink.appendChild(img);
    projectDiv.appendChild(sourceCodeLink);

    /* Add the project details below the project image */
    projectDiv.appendChild(details);

    /* Extract all information from the project, and add it to the
     * relevant element on the page */
    sourceCodeLink.href = project.source;
    blogLink.href = project.blog;
    header.innerText = project.title;
    img.src = project.preview;
    img.alt = `Screenshot of the ${project.title} project`;
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
