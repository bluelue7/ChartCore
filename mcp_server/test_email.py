import os
import smtplib
from email.mime.text import MIMEText

SMTP_SERVER = 'smtp.qq.com'
SMTP_PORT = 587
SMTP_USER = '3331603092@qq.com'
SMTP_PASSWORD = 'jmshdxwqjtbkdadf'  # 替换为你的授权码

def send_test_email():
    try:
        print("正在连接 SMTP 服务器...")
        server = smtplib.SMTP(SMTP_SERVER, SMTP_PORT)
        server.set_debuglevel(1)  # 开启调试模式
        server.starttls()
        print("正在登录...")
        server.login(SMTP_USER, SMTP_PASSWORD)
        print("登录成功")
        
        msg = MIMEText('这是一封测试邮件', 'plain', 'utf-8')
        msg['Subject'] = '测试邮件'
        msg['From'] = SMTP_USER
        msg['To'] = 'hzm_2026@163.com'
        
        print("正在发送邮件...")
        server.sendmail(SMTP_USER, 'hzm_2026@163.com', msg.as_string())
        server.quit()
        print('邮件发送成功！')
    except Exception as e:
        print(f'发送失败: {str(e)}')

if __name__ == '__main__':
    send_test_email()