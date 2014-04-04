module.exports = function(grunt) {

	grunt.loadNpmTasks('grunt-shell');
	grunt.loadNpmTasks('grunt-jsdoc');

	grunt.initConfig({
		pkg: grunt.file.readJSON('package.json'),

		jsdoc: {
			default: {
				src: ['www/bluetooth.js'],
				dest: 'doc/'
			}
		}
	});

	grunt.registerTask('default', [
		'jsdoc'
	]);
}
