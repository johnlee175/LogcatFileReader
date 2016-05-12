#!/usr/bin/env python

import urllib
import urllib2
import re
import os
import sys
import time


# upload('http://www.mywebsite.com:8080/upload.php', {}, 'file', os.path.join('/home/john/', 'a.txt'))
def upload(http_url, form_params, file_item_name, file_path):
    boundary = '-----------------%s' % hex(int(time.time() * 1000))
    crlf = '\r\n'
    separator = '--%s' % boundary
    file_type = 'application/octet-stream'
    data = []
    for key in form_params.keys():
        value = form_params[key]
        data.append(separator)
        data.append('Content-Disposition: form-data; name="%s"%s' % (key, crlf))
        data.append(value)
    data.append(separator)
    data.append('Content-Disposition: form-data; name="%s"; filename="%s"' % (file_item_name, os.path.basename(file_path)))
    data.append('Content-Type: %s%s' % (file_type, crlf))
    file_res = open(file_path)
    data.append(file_res.read())
    file_res.close()
    data.append('%s--%s' % (separator, crlf))
    http_body = crlf.join(data)
    req = urllib2.Request(http_url, data=http_body)
    req.add_header('Content-Type', 'multipart/form-data; boundary=%s' % boundary)
    req.add_header('Connection', 'Keep-Alive')
    resp = urllib2.urlopen(req, timeout=30)
    print resp.read()


# unzip('/home/john/a.zip', '/home/john/', True)
def unzip(zip_path, extract_dir, delete_zip_on_extracted):
    import zipfile
    # comment following code is because of the unix file permissions lost
    # zip_files = zipfile.ZipFile(zip_path, 'r')
    # zip_files.extractall(extract_dir)
    # zip_files.close()
    if not zipfile.is_zipfile(zip_path):
        print "%s is not a zip file" % zip_path
        exit(0)
    z = zipfile.ZipFile(zip_path)
    try:
        for info in z.infolist():
            name = info.filename
            if '..' in name:
                continue
            if name.startswith('/'):
                name = name[1:]
            target = os.path.join(extract_dir, *name.split('/'))
            if not target:
                continue
            if name.endswith('/'):  # directory
                dirname = os.path.dirname(target)
                if not os.path.isdir(dirname):
                    os.makedirs(dirname)
            else:  # file
                dirname = os.path.dirname(target)
                if not os.path.isdir(dirname):
                    os.makedirs(dirname)
                data = z.read(info.filename)
                f = open(target, 'wb')
                try:
                    f.write(data)
                finally:
                    f.close()
                    del data
            unix_attributes = info.external_attr >> 16
            if unix_attributes:
                os.chmod(target, unix_attributes)
    finally:
        z.close()
        if delete_zip_on_extracted:
            os.remove(zip_path)


# 20161201120909
def get_curr_date_str():
    return time.strftime('%Y%m%d%H%M%S', time.localtime(time.time()))


# 20161201120909
def is_valid_date_str(date_str):
    try:
        time.strptime(date_str, '%Y%m%d%H%M%S')
        return True
    except ValueError, e:
        print e
        return False


def remove_dir(top_dir):
    if os.path.exists(top_dir):
        for root, dirs, files in os.walk(top_dir, topdown=False):
            for name in files:
                os.remove(os.path.join(root, name))
            for name in dirs:
                os.rmdir(os.path.join(root, name))
        os.rmdir(top_dir)


def delete_file(src):
    if os.path.isfile(src):
        os.remove(src)
    elif os.path.isdir(src):
        for item in os.listdir(src):
            delete_file(os.path.join(src, item))
        os.rmdir(src)


# # logcat.dump.20160503082219.log
# pattern = re.compile(r'^logcat\.dump\.(\d\d\d\d\d\d\d\d\d\d\d\d\d\d)\.log$')
# def compare_file_index(a, b):
#     a_num = int(pattern.match(a).group(1))
#     b_num = int(pattern.match(b).group(1))
#     if a_num > b_num:
#         return 1
#     elif a_num < b_num:
#         return -1
#     else:
#         return 0
# merge_files('./logs/', pattern, compare_file_index)
def merge_files(folder, pattern, compare_file_index):
    print 'merge all files ...'
    file_list = []
    for parent, dir_names, file_names in os.walk(folder):
        for file_name in file_names:
            if pattern.match(file_name):
                file_list.append(file_name)
    file_list.sort(cmp=compare_file_index)
    output_path = os.path.join(folder, file_list[0])
    output_fd = open(output_path, mode='a')
    for log_file in file_list[1:]:
        log_path = os.path.join(folder, log_file)
        input_fd = open(log_path)
        data = input_fd.read()
        output_fd.write(data)
        output_fd.flush()
        input_fd.close()
        del data
        os.remove(log_path)
    output_fd.close()
    return output_path


def fetch_url_with_line(req_url):
    request = urllib2.Request(req_url)
    resp = urllib2.urlopen(request, timeout=30)
    return resp.read().splitlines()


# download(['http://www.mywebsite.com:8080/download.php?file=a.zip'], './zips/', ['a.zip'])
def download(urls, folder, file_names):
    if not os.path.exists(folder):
        os.makedirs(folder)
    for idx, url in enumerate(urls):
        print 'downloading ' + url
        file_path = os.path.join(folder, file_names[idx])
        urllib.urlretrieve(url, file_path)


# def flat_map_each_file(file_name, file_path, file_ext):
#     print 'file path is ' + file_path + ", including file name: " \
#           + file_name + ", " + file_ext + " is filename extension"
# iter_files('/home/john/logs/', flat_map_each_file)
def iter_files(top_folder, flat_map_each_file):
    for parent, dir_names, file_names in os.walk(top_folder):
        for file_name in file_names:
            file_path = os.path.join(parent, file_name)
            file_base, file_ext = os.path.splitext(file_path)
            flat_map_each_file(file_name, file_path, file_ext)


def platform_name():
    if sys.platform == 'darwin':
        return 'macosx'
    elif sys.platform == 'linux2':
        return 'linux'
    elif sys.platform.find('win') >= 0:
        return 'windows'
    else:
        return ''


def binary(name):
    if os.name == 'posix':
        return './' + name
    elif os.name == 'nt':
        return name + '.exe'
    else:
        return name

