const fs = require('fs');
const path = require('path');

// 递归查找所有Vue文件
function findVueFiles(dir, fileList = []) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const filePath = path.join(dir, file);
        const stat = fs.statSync(filePath);
        if (stat.isDirectory()) {
            findVueFiles(filePath, fileList);
        } else if (file.endsWith('.vue')) {
            fileList.push(filePath);
        }
    }
    return fileList;
}

// 移除文件中的空行
function removeEmptyLines(filePath) {
    try {
        console.log(`处理文件: ${filePath}`);
        let content = fs.readFileSync(filePath, 'utf8');

        // 记录原始行数
        const originalLines = content.split('\n').length;

        // 更严格地移除空行：移除任何只包含空白字符的行，包括那些只有空格、制表符的行
        const newContent = content
            .replace(/^[ \t]*[\r\n]/gm, '') // 移除只包含空白的行
            .replace(/\n\s*\n/g, '\n'); // 将多个连续空行合并为单个空行

        // 记录处理后的行数
        const newLines = newContent.split('\n').length;
        const removedLines = originalLines - newLines;

        // 如果内容有变化，则写回文件
        if (content !== newContent) {
            fs.writeFileSync(filePath, newContent, 'utf8');
            console.log(`已移除 ${removedLines} 行空行: ${filePath}`);
        } else {
            console.log(`无空行需要移除: ${filePath}`);
        }

        return removedLines;
    } catch (error) {
        console.error(`处理文件失败 ${filePath}:`, error);
        return 0;
    }
}

// 主函数
function main() {
    const srcDir = path.join(__dirname, 'src');
    if (!fs.existsSync(srcDir)) {
        console.error(`源目录不存在: ${srcDir}`);
        return;
    }

    console.log(`开始查找Vue文件...`);
    const vueFiles = findVueFiles(srcDir);
    console.log(`找到 ${vueFiles.length} 个Vue文件`);

    let totalRemoved = 0;
    for (const filePath of vueFiles) {
        const removed = removeEmptyLines(filePath);
        totalRemoved += removed;
    }

    console.log(`处理完成! 总共移除了 ${totalRemoved} 行空行`);
}

main(); 