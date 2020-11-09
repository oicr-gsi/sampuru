export interface Project {
  id: number,
  name: string,
  cases_total: number,
  cases_completed: number,
  qcables_total: number,
  qcables_completed: number,
  last_update: string,
}

export interface ProjectJSON {
  id: number,
  name: string,
  cases_total: number,
  cases_completed: number,
  qcables_total: number,
  qcables_completed: number,
  last_update: string,
}

/**
 * Perform a JSON fetch operation
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