import {navbar} from "./html.js";
import {initialiseActiveProjects} from "./all-projects.js";

document.body.appendChild(navbar());
initialiseActiveProjects();