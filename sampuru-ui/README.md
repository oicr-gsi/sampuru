# Getting started with client-side TypeScript
- `yarn` or `npm` to manage packages
- `npm run` to do various tasks
- `webpack` to build and serve UI development


- `package.json` contains modules to download
- `tsconfig.json` configures the TypeScript compiler `tsc`
- `webpack.config.js` configures WebPack

## Minimum version requirements

* `Java 14`
* `node >= 14`
* `npm >= 6`
* `tsc >= 3` 

## Downloading dependencies
In order to download the dependencies in package.json:

`npm install`

or 

`yarn install`


## Run scripts in package.json
npm run `script_name`


In order to transpile the TypeScript files:
`npm run build`

Output will be redirected to:
`sampuru-server/src/main/resources/static`


## Using webpack dev server to test UI changes
`npm run start`

A webpage should open with the UI as served from `index.ts`