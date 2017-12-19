console.log(`process.platform = [${process.platform}]`);
console.log('---------------------------');

const dump_args = (process.platform === 'win32') ? `${__dirname}/dump_args.cmd` : `${__dirname}/dump_args.sh`;
console.log(`dump_args executable = [${dump_args}]`);
console.log('---------------------------');

const cp = require('child_process');
const eopts = {stdio: 'inherit'};

cp.execFileSync(dump_args, ['aaa', 'bbb', '{"na \'me" : "hello, \'bob\' \\500 !"}', 'thx'], eopts);
console.log('---------------------------');

const jsonv = {
    jon: 'Hello, "Bob" ! How much your PC?',
    bob: "Hello, 'Jon' ! It's \\150,000(yen)."
}
const json = JSON.stringify(jsonv);
cp.execFileSync(dump_args, ['aaa', 'bbb', json, 'thx'], eopts);
// -> 「.\"}" の使い方が誤っています。」 oops !!
