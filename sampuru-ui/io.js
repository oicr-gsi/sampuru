/**
 * Perform a JSON fetch operation
 */
export function fetchJson(url, parameters, callback) {
    fetchOperation(url, parameters, (p) => p
        .then((response) => response.json())
        .then((response) => {
        callback(response);
    }));
}
/**
 * Perform a generic asynchronous fetch operation
 */
export function fetchOperation(url, parameters, process) {
    process(fetch(url, parameters).then((response) => {
        if (response.ok) {
            return Promise.resolve(response);
        }
        else if (response.status == 503) {
            return Promise.reject(new Error("Sampuru is currently overloaded."));
        }
        else {
            return Promise.reject(new Error(`Failed to load: ${response.status} ${response.statusText}`));
        }
    }))
        .catch((error) => alert(error.nessage)); //TODO: better error handling
}
