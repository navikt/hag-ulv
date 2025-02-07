const getMaskinportenToken = require("../old-getMaskinportenToken.js");

console.error("From the outside!");

getMaskinportenToken("maskinporten-hag-lps-api-client-").then((result) => {console.log(result)});