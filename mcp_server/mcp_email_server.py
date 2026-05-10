#!/usr/bin/env python3
"""
MCP Email Server - 实现 MCP 协议的邮件发送服务
"""

import json
import sys
import smtplib
import os
import traceback
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.header import Header

# 确保 stderr 编码正确
# sys.stderr = open(sys.stderr.fileno(), 'w', encoding='utf-8', closefd=False)

# 强制 UTF-8 编码
# sys.stdin.reconfigure(encoding='utf-8')
# sys.stdout.reconfigure(encoding='utf-8')

# 读取环境变量
SMTP_SERVER = os.environ.get('SMTP_SERVER', 'smtp.qq.com')
SMTP_PORT = int(os.environ.get('SMTP_PORT', 587))
SMTP_USER = os.environ.get('SMTP_USER', '')
SMTP_PASSWORD = os.environ.get('SMTP_PASSWORD', '')

# def log(msg):
#     """输出日志到 stderr"""
#     print(f"[MCP] {msg}", file=sys.stderr, flush=True)

def send_email(to_email: str, subject: str, html_content: str) -> bool:
    try:
        # log(f"Sending email to: {to_email}")

        msg = MIMEMultipart('alternative')
        msg['From'] = Header('Chart System <' + SMTP_USER + '>', 'utf-8')
        msg['To'] = Header(to_email, 'utf-8')
        msg['Subject'] = Header(subject, 'utf-8')

        html_part = MIMEText(html_content, 'html', 'utf-8')
        msg.attach(html_part)

        # log(f"Connecting to {SMTP_SERVER}:{SMTP_PORT}")
        server = smtplib.SMTP(SMTP_SERVER, SMTP_PORT)
        # log("Starting TLS...")
        server.starttls()
        # log(f"Logging in as {SMTP_USER}")
        server.login(SMTP_USER, SMTP_PASSWORD)
        # log("Sending email...")
        server.sendmail(SMTP_USER, to_email, msg.as_string())
        server.quit()

        # log("Email sent successfully")
        return True
    except Exception as e:
        # log(f"Error sending email: {str(e)}")
        # traceback.print_exc(file=sys.stderr)  # 打印完整错误堆栈
        return False

def handle_request(request: dict) -> dict:
    try:
        request_type = request.get('type')

        if request_type == 'initialize':
            return {
                "type": "initialize_response",
                "capabilities": {
                    "tools": [
                        {
                            "name": "send_email",
                            "description": "send email",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "to_email": {"type": "string"},
                                    "subject": {"type": "string"},
                                    "html_content": {"type": "string"}
                                },
                                "required": ["to_email", "subject", "html_content"]
                            }
                        }
                    ]
                }
            }

        elif request_type == 'tool_call':
            tool_name = request.get('name')
            args = request.get('arguments', {})
            if tool_name == 'send_email':
                to_email = args.get('to_email')
                subject = args.get('subject')
                html_content = args.get('html_content')
                success = send_email(to_email, subject, html_content)
                return {
                    "type": "tool_result",
                    "success": success
                }

        return {"type": "error"}
    except Exception:
        return {"type": "error"}

def main():
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
                success = send_email(
                    args.get('to_email', ''),
                    args.get('subject', ''),
                    args.get('html_content', '')
                )
                resp = {
                    'type': 'tool_result',
                    'success': success
                }
            else:
                resp = {'type': 'error'}
            
            print(json.dumps(resp))
            sys.stdout.flush()
        except:
            print(json.dumps({'type': 'error'}))
            sys.stdout.flush()

if __name__ == '__main__':
    main()