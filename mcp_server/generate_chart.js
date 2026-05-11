const { createCanvas } = require('canvas');
const echarts = require('echarts');

/**
 * 使用 ECharts 和 Canvas 生成图表图片
 */
function generateChart(option, width = 800, height = 500) {
    // 创建 Canvas
    const canvas = createCanvas(width, height);
    
    // 创建一个模拟的 DOM 对象，包含 Canvas
    const dom = {
        getBoundingClientRect: () => ({ width, height }),
        style: {},
        appendChild: () => {},
        removeChild: () => {},
        ownerDocument: {
            createElement: () => canvas
        }
    };
    
    // 初始化 ECharts，传递模拟 DOM
    const chart = echarts.init(dom, null, {
        renderer: 'canvas',
        useDirtyRect: false
    });
    
    // 设置图表尺寸
    chart.resize({ width, height });
    
    // 设置图表配置
    chart.setOption(option);
    
    // 获取图片数据
    const imageData = chart.getDataURL({
        type: 'png',
        pixelRatio: 1,
        backgroundColor: '#fff'
    });
    
    // 清理
    chart.dispose();
    
    // 返回 Buffer
    return Buffer.from(imageData.split(',')[1], 'base64');
}

// 从命令行参数获取图表配置
const args = process.argv.slice(2);
if (args.length < 1) {
    console.error('请提供图表配置 JSON');
    process.exit(1);
}

try {
    const option = JSON.parse(args[0]);
    const buffer = generateChart(option);
    process.stdout.write(buffer);
} catch (error) {
    console.error(`生成图表失败: ${error.message}`);
    process.exit(1);
}