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
    .catch((error) => alert(error.message)); //TODO: better error handling
}


export function urlConstructor(destination: string, params: string[], values: string[]) {
  const newUrl = new URL(window.location.origin + "/"+ destination);
  const queryParams = new URLSearchParams(newUrl.search);
  params.forEach((param, index, paramArr) => {
    queryParams.set(param, values[index]);
  });

  newUrl.search = queryParams.toString();
  return newUrl.toString();
}
