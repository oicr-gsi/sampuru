import {Project, ProjectJSON} from "./data-transfer-objects";

/**
 * Perform JSON fetch operation with a callback
 */
export function fetchJson<T>(
  url: RequestInfo,
  parameters: RequestInit,
  callback: (result: T) => void
): void {
  fetchOperation(url, parameters, (p) =>
    p
      .then((response) => response.json())
      .then((response) => {
        callback(response as T);
      })
  );
}

/**
 *
 * @param url
 * @param parameters
 *
 * Return promise
 */
export function fetchAsPromise<T> (
  url: RequestInfo,
  parameters: RequestInit
): Promise<T> {
  return fetch(url, parameters)
    .then((response) => {
      if (response.ok) {
        return Promise.resolve(response);
      } else if (response.status == 503) {
        return Promise.reject(new Error("Sampuru is currently overloaded."));
      } else {
        return Promise.reject(
          new Error(`Failed to load: ${response.status} ${response.statusText}`)
        );
      }
    })
    .then((response) => response.json())
    .then((response) => response as T);
}


/**
 * Perform a generic asynchronous fetch operation
 */
export function fetchOperation<T>(
  url: RequestInfo,
  parameters: RequestInit,
  process: (promise: Promise<Response>) => Promise<T>
): void {
  process(
    fetch(url, parameters).then((response) => {
      if (response.ok) {
        return Promise.resolve(response);
      } else if (response.status == 503) {
        return Promise.reject(new Error("Sampuru is currently overloaded."));
      } else {
        return Promise.reject(
          new Error(`Failed to load: ${response.status} ${response.statusText}`)
        );
      }
    })
  )
    .catch((error) => alert(error.nessage)); //TODO: better error handling
}

/**
 * Convert ProjectJson object to Project
* */
export function decodeProject(json: ProjectJSON): Project {
  return Object.assign({}, json, {
    last_update: new Date(json.last_update)
  });
}