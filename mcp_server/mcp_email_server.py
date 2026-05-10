#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MCP Email Server - Debug Version
"""

import json
import sys
import smtplib
import os
from email.mime.text import MIMEText
from email.header import Header
import smtplib

# 读取环境变量
SMTP_SERVER = os.environ.get('SMTP_SERVER', 'smtp.qq.com')
SMTP_PORT = int(os.environ.get('SMTP_PORT', 587))
SMTP_USER = os.environ.get('SMTP_USER', '')
SMTP_PASSWORD = os.environ.get('SMTP_PASSWORD', '')

def clean_text(text):
    """清理无效的 Unicode 字符"""
    if not text:
        return ''
    
    # 移除代理对和无效字符
    cleaned = []
    i = 0
    while i < len(text):
        code = ord(text[i])
        # 跳过代理对 (surrogate pairs)
        if 0xD800 <= code <= 0xDFFF:
            i += 1
        else:
            cleaned.append(text[i])
        i += 1
    
    return ''.join(cleaned)

def send_email(to_email, subject, html_content):
    """发送邮件并返回结果和错误信息"""
    try:
        # 检查配置
        if not SMTP_USER or not SMTP_PASSWORD:
            return False, "SMTP_USER or SMTP_PASSWORD not set"
            
        # 清理无效的 Unicode 字符
#         to_email = clean_text(to_email)
#         subject = clean_text(subject)
#         html_content = clean_text(html_content)
        
        msg = MIMEText(html_content, 'html', 'utf-8')
        msg['From'] = SMTP_USER
        msg['To'] = to_email
        msg['Subject'] = Header(subject, 'utf-8')
        
        server = smtplib.SMTP(SMTP_SERVER, SMTP_PORT, timeout=30)
        
        try:
            server.starttls()
            server.login(SMTP_USER, SMTP_PASSWORD)
            server.sendmail(SMTP_USER, to_email, msg.as_string())
        finally:
            server.quit()
        
        return True, "Success"
        
    except smtplib.SMTPAuthenticationError:
        return False, "Authentication failed - check username/password"
    except smtplib.SMTPConnectError:
        return False, f"Connection failed to {SMTP_SERVER}:{SMTP_PORT}"
    except Exception as e:
        return False, f"Error: {type(e).__name__}: {str(e)}"

def main():
    # 确保 stdin/stdout 使用 utf-8
    sys.stdin = open(sys.stdin.fileno(), 'r', encoding='utf-8', closefd=False)
    sys.stdout = open(sys.stdout.fileno(), 'w', encoding='utf-8', closefd=False)
    while True:
        line = sys.stdin.readline()
        if not line:
            break
            
        line = line.strip()
        if not line:
            continue
        
        try:
            req = json.loads(line)
            req_type = req.get('type')
            
            if req_type == 'initialize':
                resp = {
                    'type': 'initialize_response',
                    'capabilities': {
                        'tools': [{
                            'name': 'send_email',
                            'description': 'Send email',
                            'parameters': {
                                'type': 'object',
                                'properties': {
                                    'to_email': {'type': 'string'},
                                    'subject': {'type': 'string'},
                                    'html_content': {'type': 'string'}
                                },
                                'required': ['to_email', 'subject', 'html_content']
                            }
                        }]
                    }
                }
            elif req_type == 'tool_call':
                args = req.get('arguments', {})
                success, message = send_email(
                    args.get('to_email', ''),
                    args.get('subject', ''),
                    args.get('html_content', '')
                )
                resp = {
                    'type': 'tool_result',
                    'success': success,
                    'content': message  # 添加错误信息
                }
            else:
                resp = {'type': 'error', 'content': 'Unknown request type'}
            
            print(json.dumps(resp))
            sys.stdout.flush()
        except Exception as e:
            resp = {'type': 'error', 'content': f'Error: {str(e)}'}
            print(json.dumps(resp))
            sys.stdout.flush()

if __name__ == '__main__':
    main()