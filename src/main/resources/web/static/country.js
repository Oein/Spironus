const isLocalTest = localStorage.getItem("localtest") === "true";
const url = new URL(window.location.href);
const tk = url.searchParams.get("token") || localStorage.getItem("token");
if (!tk && !isLocalTest) {
  alert("You need to provide a token to access this page.");
  window.location.href = "/index.html";
}
const lib = {};
