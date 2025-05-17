const fs = require('fs');
const path = require('path');

// Function to recursively find all Vue files in a directory
function findVueFiles(dir, fileList = []) {
    const files = fs.readdirSync(dir);

    files.forEach(file => {
        const filePath = path.join(dir, file);
        const stat = fs.statSync(filePath);

        if (stat.isDirectory()) {
            findVueFiles(filePath, fileList);
        } else if (file.endsWith('.vue')) {
            fileList.push(filePath);
        }
    });

    return fileList;
}

// Function to remove empty lines from a file
function removeEmptyLines(filePath) {
    try {
        let content = fs.readFileSync(filePath, 'utf8');

        // Remove empty lines (lines with only whitespace)
        content = content.replace(/^\s*[\r\n]/gm, '');

        // Write the content back to the file
        fs.writeFileSync(filePath, content, 'utf8');

        console.log(`Processed: ${filePath}`);
    } catch (error) {
        console.error(`Error processing ${filePath}: ${error.message}`);
    }
}

// Directory to search for Vue files
const srcDir = path.join(__dirname, 'src');

// Find all Vue files
const vueFiles = findVueFiles(srcDir);

console.log(`Found ${vueFiles.length} Vue files to process`);

// Process each file
vueFiles.forEach(file => {
    removeEmptyLines(file);
});

console.log('Finished removing empty lines from all Vue files'); 