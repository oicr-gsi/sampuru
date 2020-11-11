"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var html_js_1 = require("./html.js");
var all_projects_js_1 = require("./all-projects.js");
document.body.appendChild(html_js_1.navbar());
all_projects_js_1.initialiseActiveProjects();
