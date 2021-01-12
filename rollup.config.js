import resolve from '@rollup/plugin-node-resolve'

export default {
  input: 'sampuru-server/src/main/resources/static/project.js',
  output: {
    file: 'sampuru-server/src/main/resources/static/project.js',
    format: 'umd',
    name: 'ProjectOverview'
  },
  plugins: [resolve({
    // pass custom options to the resolve plugin
    moduleDirectories: ['node_modules']
  })]
}