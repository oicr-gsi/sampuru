"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.decodeProject = exports.fetchOperation = exports.fetchAsPromise = exports.fetchJson = void 0;
/**
 * Perform JSON fetch operation with a callback
 */
function fetchJson(url, parameters, callback) {
    fetchOperation(url, parameters, function (p) {
        return p
            .then(function (response) { return response.json(); })
            .then(function (response) {
            callback(response);
        });
    });
}
exports.fetchJson = fetchJson;
/**
 *
 * @param url
 * @param parameters
 *
 * Return promise
 */
function fetchAsPromise(url, parameters) {
    return fetch(url, parameters)
        .then(function (response) {
        if (response.ok) {
            return Promise.resolve(response);
        }
        else if (response.status == 503) {
            return Promise.reject(new Error("Sampuru is currently overloaded."));
        }
        else {
            return Promise.reject(new Error("Failed to load: " + response.status + " " + response.statusText));
        }
    })
        .then(function (response) { return response.json(); })
        .then(function (response) { return response; });
}
exports.fetchAsPromise = fetchAsPromise;
/**
 * Perform a generic asynchronous fetch operation
 */
function fetchOperation(url, parameters, process) {
    process(fetch(url, parameters).then(function (response) {
        if (response.ok) {
            return Promise.resolve(response);
        }
        else if (response.status == 503) {
            return Promise.reject(new Error("Sampuru is currently overloaded."));
        }
        else {
            return Promise.reject(new Error("Failed to load: " + response.status + " " + response.statusText));
        }
    }))
        .catch(function (error) { return alert(error.nessage); }); //TODO: better error handling
}
exports.fetchOperation = fetchOperation;
/**
 * Convert ProjectJson object to Project
* */
function decodeProject(json) {
    return Object.assign({}, json, {
        last_update: new Date(json.last_update)
    });
}
exports.decodeProject = decodeProject;
