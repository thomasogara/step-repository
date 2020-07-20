const fetch_footer = async () => {
  const path = "footer.html";
  const data = await fetch(path);
  const text = await data.text();
  const footer = document.getElementById('footer-navbar');
  footer.innerHTML = text
}

fetch_footer();
