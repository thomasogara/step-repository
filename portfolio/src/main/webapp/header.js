const fetch_header = async () => {
  const path = "header.html";
  const data = await fetch(path);
  const text = await data.text();
  const header = document.getElementById('header-navbar');
  header.innerHTML = text;
}

fetch_header();
