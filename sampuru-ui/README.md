# Getting started with client-side TypeScript
- `yarn` to manage packages
- `npm run` to do various tasks
- `webpack` to build and serve UI development

- `package.json` contains modules to download
- `tsconfig.json` configures the TypeScript compiler `tsc`
- `webpack.config.js` configures WebPack

## Downloading dependencies
In order to download the dependencies in package.json:
yarn install 

or 

npm install

## Run scripts in package.json
npm run `script_name`
So execute the build script:
npm run build

This will transpile the TypeScript into js files that are outputted in 
`sampuru-server/src/main/resources/ca/on/oicr/gsi/sampuru`

For the Server to read 

## Using webpack dev server to test UI changes
npm run start

A webpage should open with the UI as served from index.ts