const { exec } = require('child_process');
const { promisify } = require('util');
const path = require('path');
const execAsync = promisify(exec);


async function getMaskinportenToken(serviceName) {

    const directJarPath = '../maskinporten-token-jar/build/libs/hag-api-testing-katalog-1.0-SNAPSHOT.jar'
    // const serviceName = "maskinporten-hag-lps-api-client-"

    try {

        const command = process.platform === 'win32' ? 'where java' : 'which java';
        const { stdout: javaPath } = await execAsync(command);
        const cleanJavaPath = javaPath.trim();

        console.log('PATH:', process.env.PATH);
        console.log('JAVA_HOME:', process.env.JAVA_HOME);

        const scriptDir = __dirname;

        const jarPath = path.join(scriptDir, directJarPath);

        const { stdout, stderr } = await execAsync(`"${cleanJavaPath}" -jar "${jarPath}" "${serviceName}"`, {
            cwd: scriptDir
        });

        if (stderr) console.error('Errors:', stderr);

        return stdout
    } catch (error) {
        console.error('Error:', error);
    }
}


getMaskinportenToken("maskinporten-hag-lps-api-client-").then((result) => {console.log(result)});