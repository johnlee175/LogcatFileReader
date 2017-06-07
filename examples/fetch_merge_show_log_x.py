#!/usr/bin/env python


import re
import os
import sys
import simple_utils


_VERSION = '1.0.0'
_BASE_URL = 'http://www.yourwebsite.com:8080/logs/'


# change this method for your custom
def fetch_urls(uid, date):
    result_urls = []
    href_pattern = re.compile(r'.*?<a\shref="(logcat\.dump\.(\d\d\d\d\d\d\d\d\d\d\d\d\d\d)\.log.*?)">logcat\.dump\.\d+\.log.+')
    req_url = _BASE_URL + uid
    for line in simple_utils.fetch_url_with_line(req_url):
        match = href_pattern.match(line)
        if match:
            if int(match.group(2)) >= int(date):
                result_urls.append(req_url + '/' + match.group(1))
    return result_urls


def unpack(logs_folder):
    print 'unzip all log files ...'
    for parent, dir_names, file_names in os.walk(logs_folder):
        for file_name in file_names:
            full_path = os.path.join(parent, file_name)
            file_base, file_ext = os.path.splitext(full_path)
            if file_ext == '.zip':
                if os.path.exists(file_base):
                    os.remove(file_base)
                simple_utils.unzip(full_path, logs_folder, True)


# logcat.dump.20160503082219.log
pattern = re.compile(r'^logcat\.dump\.(\d\d\d\d\d\d\d\d\d\d\d\d\d\d)\.log$')


def compare_file_index(a, b):
    a_num = int(pattern.match(a).group(1))
    b_num = int(pattern.match(b).group(1))
    if a_num > b_num:
        return 1
    elif a_num < b_num:
        return -1
    else:
        return 0


def show_log(log_path):
    cmd = 'java -jar LogcatFileReader-2.0.0.jar ' + log_path + ' threadtime'
    print 'exec ' + cmd
    os.system(cmd)


def parse_opt(argv):
    import optparse
    usage = 'Usage: %prog [[options] [value]]'
    desc = 'Example: %prog -u 2060900675 -d 20160420153000'
    parser = optparse.OptionParser(usage=usage, description=desc)
    parser.add_option('-v', dest='version', action='store_true', help='show the current version')
    parser.add_option('-u', dest='user_id', type='string', help="special the device user id",
                      default=' ', metavar='USER_ID')
    parser.add_option('-d', dest='date', type='string', help='which day of log do you want to see',
                      default=simple_utils.get_curr_date_str(), metavar='DATE_STRING')
    options, categories = parser.parse_args(argv[1:])
    return options.version, options.user_id, options.date


def main(argv):
    logs_folder = 'logs'
    version, user_id, date = parse_opt(argv)
    if version:
        print 'The Current Version: ' + _VERSION
        return
    if not user_id or len(user_id.strip()) == 0:
        print 'User id required, please with -u {USER_ID}'
        return
    if not simple_utils.is_valid_date_str(date):
        print 'The date string provided by -d {DATE_STRING} is invalid, accurate to seconds, like 20160101053000'
        return
    simple_utils.remove_dir(logs_folder)
    url_list = fetch_urls(user_id, date)
    if not url_list:
        print 'No new log files be matched from remote site'
    simple_utils.download(url_list, logs_folder, [url[(url.rindex('/') + 1):] for url in url_list])
    unpack(logs_folder)
    show_log(simple_utils.merge_files(logs_folder, pattern, compare_file_index))


if __name__ == '__main__':
    main(sys.argv)

