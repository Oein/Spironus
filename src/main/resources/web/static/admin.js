const isLocalTest = localStorage.getItem("localtest") === "true";
const lib = {};

if (isLocalTest) document.getElementById("dev").style.display = "block";
