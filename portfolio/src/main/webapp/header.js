const fetch_header = async () => {
  const path = "header.html";
  const data = await fetch(path);
  const text = await data.text();
  const header = document.getElementById('navbar-list');
  header.innerHTML = text;
}

fetch_header();
