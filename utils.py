# -*- coding: utf-8 -*-
import os
import requests, json, io, base64, os
import mdtex2html
from PIL import Image
import gradio as gr
from promptgen import *
import time
import random
from PIL import Image
import base64
import cv2
import openai
import numpy as np
from fastapi import FastAPI, UploadFile, Form, Depends
from dataclasses import dataclass
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from typing import List, Dict
import uuid
import re
import glob
import ast

ART_ADVICE = "你是一个艺术家。以“您可以这样画这幅画”开头回复，在100字以内，不要给出多种场景。有以下几种讨论情况和回复方式：1.用户说了一个数字（1,2,3），请复述该数字对应主题的内容；2.用户没有接受任何一个推荐的主题，没说序号，而是提出了自己的绘画意图，请根据用户指令进行绘画描述，不要参考推荐主题的内容；3.用户提出自己的绘图意图，一定要根据他的想法，做出简要图像描述表示赞同；4.用户询问你的建议，你就基于用户指令和之前绘画主题或者修改图片讨论的最终结果，给用户一个绘画描述。不要说“我不具备展示、生成图片的能力；您的要求和我的功能不相符；我无法接受图片信息”，让用户认为你知道图片的内容。不要加双引号。"
UPLOAD_ADVICE = "你是一个专业的艺术评论家。给你关于用户图片的文字描述，你要先回复“收到图片”，接着另起一段，复述这段文字描述。然后另起一段，根据收到的文字描述，最好从增减或改变背景中的物体、变换绘画风格出发，提出专业有想象力的改进建议，不要有对比度、层次感这方面的建议。不要说“从你的描述中，您提到图片中”，而是要说“根据您上传的图片”这种类似的话。你要让用户认为图片是你自己理解的"

# ART_ADVICE = "You are a professional art critic. If a user asks for your advice, provide a painting description for inspiration based on the previous chat record, starting with 'You could paint this picture like this', be imaginative, and LIMIT IT TO 120 WORDS without offering multiple scenarios; if the user suggests their own drawing idea, give a concise response to show agreement. DON'T SAY 'I lack the capability to generate images'."
# UPLOAD_ADVICE = "You are a professional art critic. Upon receiving a textual description of an image, you should first respond with 'Received', followed by a separate paragraph restating this textual description. Then, in another separate paragraph, based on the received text description, it would be best to provide professional and imaginative improvement suggestions, primarily considering adding, reducing, or altering objects in the background or changing the painting style. Avoid giving advice on contrast and depth of field. DON'T SAY 'From your description, you mentioned in the picture', but rather use phrases similar to 'Based on the image you uploaded'. Make the user believe that the image is understood by you."
CN_TXT2IMG_PROMPT = "ONLY WRITE ENGLISH PROMPT. You are to receive an art discussion between a user and an artist. Use the FINAL RESULT of the discussion. You need to depict the SCENE of the NEW IMAGE from these perspectives as an ENGLISH prompt for the text-to-image model: main characters or objects; background objects; style. Summarize the improvements to the image after the art discussion, but also retain parts of the original image that were not modified. The prompt should NOT EXCEED 50 words and should not include terms like 'high contrast'. When replying, provide only the ENGLISH PROMPT and DON'T USE quotation marks."
TXT2IMG_NEG_PROMPT = "ONLY WRITE ENGLISH PROMPT. You are provided with an art discussion between user and artist. Use the FINAL RESULT of the discussion. If the user mentions the people, objects, scenes, or styles they wish to paint, summarize the antonyms of what they want to paint into ENGLISH keywords, not exceeding 6 words. If the user does not specify what they don't want to paint, reply with a space. For instance, if the user doesn't want to paint nighttime, your response should be 'night scene'; if the user wants to paint nighttime, your response should be 'daytime'. DON'T USE quotation marks, and don't start with words like 'create' or 'paint'"
TXT2IMG_PROMPT = "ONLY WRITE ENGLISH PROMPT. Give you art discussions between the user and the artist. Use the FINAL RESULT of the discussion. If the user believes the artist's description of the image is incorrect, you should comply with the user's request. Place the painting theme chosen by the user at the beginning and write ENGLISH prompt for the text-to-image model to draw a picture, within 50 words. Note that if the description is relatively long, you need to extract the main imagery and scenes; if short, make sure to emphasize the subject of the painting, employ your imagination, and add some content to enrich the details. DON'T add quotation marks, and DON'T begin with words like 'create' or 'paint', just directly describe the scene."
TRANSLATE = "Translate this Chinese text into English."

TOPIC_RECOMMEND_1 = "回答格式：直接写绘画主题，不加引号。你是一个想象力丰富的艺术家，给你用户绘画指令和用户所处的情境，分析出用户最有可能的创作意图，推荐一个绘画主题，不超过20字。请遵从用户的绘画指令，同时可添加额外的信息以丰富画面"
# TOPIC_RECOMMEND_1 = "Answer format example:[painting theme here, don't use brackets[]]. You are an imaginative artist. Given the painting User Command and the context of the user, analyze the MOST LIKELY PAINTING INTENTION, provide 1 painting theme, in one sentence of NO MORE THAN 20 WORDS. FOLLOW THE USER COMMAND, but additional information can be added to enrich the imagery."
TOPIC_RECOMMEND_2 = "回答格式：1.绘画主题1。\n2.绘画主题2。你是一个想象力丰富的艺术家，给你用户绘画指令和用户所处的情境，分析出用户最有可能的创作意图，推荐两个绘画主题，每个主题不超过20字。请遵从用户的绘画指令，同时可添加额外的信息以丰富画面"
# TOPIC_RECOMMEND_2 = "Answer format example:1.[painting theme 1 here, don't use brackets[]]\n2.[painting theme 2 here, don't use brackets[]]. You are an imaginative artist. Given the painting User Command and the context of the user, analyze the MOST LIKELY PAINTING INTENTION, provide 2 painting themes, each theme in one sentence of NO MORE THAN 20 WORDS. FOLLOW THE USER COMMAND, but additional information can be added to enrich the imagery."
EDIT_TOPIC_1 = "回答格式：直接写修改建议。你是一个想象力丰富的艺术家，请分析出用户最有可能的修改图片意图，推荐1个修改主题，不超过20字。从这5个方向中选择1个提出建议，只描绘新图片的场景：原图是人像，则换成新的绘画风格，如卡通、科幻、油画、水彩、国画、复古、印象派；增减、更换原图中的人物/动物/物体/背景；根据原图风格生成新的场景不同的图片；根据原图人物姿势生成相同姿势不同人物或动物的图片；如果原图是建筑物或室内，生成精致的装饰设计图。不要涉及对比度、深度这种词汇。"
# EDIT_TOPIC_1 = "Answer format example:[theme here, don't use brackets[]]. You are a creative artist. Determine the most likely intention of the user in editing the painting. From the following 5 options, select 1 to offer image modification suggestions and DESCRIBE THE NEW IMAGE SCENE: considering the addition, removal, or modification of background objects; style changes; generating new images based on the original style; creating new images based on the posture of the person in the original; if the original image is indoors or features buildings, produce a detailed design drawing. Exclude suggestions on contrast and depth. Offer one editing theme within a 20-WORD LIMIT, following the user's instruction, but enhance with supplementary details."
EDIT_TOPIC_2 = "回答格式：1.修改建议1。\n2.修改建议2。你是一个想象力丰富的艺术家，请分析出用户最有可能的修改图片意图，推荐2个修改主题，每个主题不超过20字。每次从这5个方向中选择1个提出建议，只描绘新图片的场景：原图是人像，则换成新的绘画风格，如卡通、科幻、油画、水彩、国画、复古、印象派；增减、更换原图中的人物/动物/物体/背景；根据原图风格生成新的场景不同的图片；根据原图人物姿势生成相同姿势不同人物/动物的图片；如果原图是建筑物或室内，生成精致的装饰设计图。不要涉及对比度、深度这种词汇。"
# EDIT_TOPIC_2 = "Answer format example:1.[theme 1 here, don't use brackets[]]\n2.[theme 2 here, don't use brackets[]]. You are a creative artist. Determine the most likely intention of the user in editing the painting. From the following 5 options, each theme select 1 to offer image modification suggestions and DESCRIBE THE NEW IMAGE SCENE: considering the addition, removal, or modification of background objects; style changes; generating new images based on the original style; creating new images based on the posture of the person in the original; if the original image is indoors or features buildings, produce a detailed design drawing. Exclude suggestions on contrast and depth. Provide 2 editing themes, each theme within a 20-WORD LIMIT, adhering to the user's directive, but enriching with additional information."
TOPIC_INTRO = "根据您的绘画指令和所处的情境，我向您推荐三个绘画主题。请选择其中一个主题开始您的创作。如果您有更好的绘画建议，请提出。\n\n"
# TOPIC_INTRO = "Based on your painting instruction and context, I recommend the following 3 painting themes. Please CHOOSE ONE to proceed with your creation. If you have a better suggestion, please share it.\n\n"
EDIT_INTRO = "根据您上传的图片和您所处的情境，我向您推荐三个修改图片的主题。请选择其中一个主题，对图片进行修改。如果您有更好的修改建议，请提出。\n\n"
MODE_DECIDE = """I will give you information on the user in 6 modalities: Location, Phone Content, Facial Expression, Weather, Music, User Command. There are 8 main scenarios for user AI painting, please judge the user's scenario and output a 5-dimensional vector, where each coordinate is represented by 0 or 1. You should directly respond with the VALUE of the VECTOR, NO EXPLANATION NEEDED, like '[0,0,0,0,0]'.
Scenario 1 (Normal Mode): vector=[0,0,0,0,0].
Scenario 2 (Work Mode for Visual Artist): The location is often residential buildings, schools, and art galleries or other life or art places. The User command often contains professional art vocabulary. vector=[0,0,1,1,1].
Scenario 3 (Work Mode for Textual Creator): The location is often residential buildings, office buildings, schools, coffee shops, and other life and office places. Phone Content is often articles, poetry, and speeches, and the user usually wants to illustrate the articles in the Phone Content. The Emotion is often neutral, vector=[0,1,0,0,0].
Scenario 4 (Work Mode for Architect): The location is often outdoors (next to buildings or parks), and the User command is often about architectural design or environmental art design, vector=[1,0,0,1,0].
Scenario 5 (Travel Mode): The location is often famous attractions, and the User command may be related to drawing attractions, vector=[1,0,1,1,1].
Scenario 6 (Music Mode): The location is often bars, concert halls, coffee shops, residential buildings and other entertainment and life places. The Music is not empty, vector=[1,0,1,1,1].
Scenario 7 (Facial Expression Mode): The User command is often related to Facial Expression, vector=[0,0,1,0,0].
Scenario 8 (Weather Mode): The User command is often related to Weather, vector=[0,0,0,1,0]."""
EDIT_TOOLS = """Choose the most appropriate image modification tool based on previous discussion and JUST OUTPUT THE NUMBER (1-5):
1. Shuffle: APPLY the STYLE of the input image to a new image.
2. Softedge_hed: CHANGE the artistic STYLE of the image without adding or removing objects from the image.
3. Depth: Add or remove objects in the image.
4. Openpose: Create a new image with the SAME POSE as the person in the original image.
5. Mlsd: Generate ARCHITECTURAL or INTERIOR DESIGN drawings based on the original image."""

# uvicorn utils:app --reload
# uvicorn utils:app --reload --port 22231 --host 0.0.0.0 --timeout-keep-alive 600 --ws-ping-timeout 600  默认是8000端口，可以改成别的，设置超时为10分钟
# daphne -u /tmp/daphne.sock -p 22231 utils:app
# ionia 开放端口：22231-22300
# http://127.0.0.1:8000/docs 是api文档

def extract_lists(text):  # 把gpt-4输出的standard vector转为list
    matches = re.findall("\[.*?\]", text)
    # 将找到的匹配项转换为实际的列表
    lists = [ast.literal_eval(match) for match in matches]
    return lists[0]

def filter_context(text, vector):  # 对空格不敏感，但一定要用英文的逗号
    sections = ["Location", "Phone-Content", "Facial Expression", "Weather", "Music", "User command"]
    text_parts = re.split("(Location:|Phone-Content:|Facial Expression:|Weather:|Music:|User command:)", text)
    
    new_text_parts = []
    for i in range(1, len(text_parts), 2):
        section = text_parts[i][:-1]
        content = text_parts[i+1].split(',')[0] if i+1 < len(text_parts) else text_parts[i+1]
        if (section != "User command" and vector[sections.index(section)] == 1 and content != "[]") or section == "User command":
            new_text_parts.append(section + ':' + content)
    
    return ','.join(new_text_parts)

def flip_random_bit(vector):
    vector_copy = vector.copy()
    # 随机选择一个索引
    index = random.choice(range(len(vector_copy)))
    # 反转选择的位
    vector_copy[index] = 1 - vector_copy[index]
    return vector_copy

def write_json(userID, *args):
    with open('output/' + userID + '.json', 'a', encoding='utf-8') as f:
        for arg in args:
            json.dump(arg, f, ensure_ascii=False)  # False，可保存utf-8编码
            f.write('\n')

def save_userID_image(user_id, img):
    """
    保存图片到特定用户的文件夹下。如果该用户的文件夹下已经有4张图片，那么新的图片将命名为5.jpg，返回"5"
    image = Image.open('input.jpg')
    save_userID_image('123', image)
    """
    path = os.path.join('output', user_id)
    os.makedirs(path, exist_ok=True)  # 如果文件夹不存在，那么创建它
    existing_images = glob.glob(os.path.join(path, '*.jpg'))
    new_image_name = str(len(existing_images) + 1) + '.jpg'
    img.save(os.path.join(path, new_image_name))
    return str(len(existing_images) + 1)

class ChatbotData(BaseModel):
    input: str
    history: List[Dict[str, str]]
    userID: str

app = FastAPI()
app.mount("/static", StaticFiles(directory="static"), name="static")
# 可以通过 URL /static/image.jpg 来访问文件
@app.post("/gpt4_predict")  # 只有data.history满足gpt-4的api格式，不能污染它
def gpt4_predict(data: ChatbotData):
    print(f"before:{data.history}")
    res = gpt4_api(ART_ADVICE, data.history)
    assistant_output = construct_assistant(res)
    data.history.append(assistant_output)

    write_json(data.userID, assistant_output)
    print(data.history)
    return {"history": data.history}

class ImageRequest(BaseModel):
    history: List[Dict[str, str]]
    userID: str
    cnt: int
    width: int
    height: int

@app.post("/gpt4_sd_draw")
def gpt4_sd_draw(data: ImageRequest):
    tmp_history = data.history
    if len(data.history) > 0:  # 去掉绘画指令那一句
        data.history.pop()
    pos_prompt = gpt4_api(TXT2IMG_PROMPT, data.history)
    print(f"pos_prompt: {pos_prompt}")
    neg_prompt = gpt4_api(TXT2IMG_NEG_PROMPT, data.history)
    print(f"neg_prompt: {neg_prompt}")
    data.history = tmp_history
    new_images, imageID = call_sd_t2i(data.userID, pos_prompt, neg_prompt, data.width, data.height)
    
    new_image = new_images[0]
    static_path = "static/images/" + str(uuid.uuid4()) + ".jpg"
    print("图片链接 http://166.111.139.116:22231/" + static_path)
    # print("图片链接 http://localhost:8000/" + static_path)
    new_image.save(static_path)
    # 构造URL
    image_url = "http://166.111.139.116:22231/" + static_path

    if data.cnt > 0:
        data.history.append(construct_user("This image doesn't align with my vision, please revise the description."))
        data.history.append(construct_assistant("My apologies, I will amend the description and generate a new image."))
        write_json(data.userID, construct_user("This image doesn't align with my vision, please revise the description."), construct_assistant("My apologies, I will amend the description and generate a new image."))
    data.cnt = data.cnt + 1

    response = call_visualglm_api(np.array(new_image))["result"]
    # response = turbo_api(TRANSLATE, [construct_user(call_visualglm_api(np.array(new_image))["result"])])

    data.history.append(construct_assistant(f"本张图片的 ImageID 是 {imageID}。\n\n{response}"))
    # data.history.append(construct_assistant(f"ImageID is {imageID}.\n\n{response}"))
    write_json(data.userID, construct_prompt(pos_prompt + "\n" + neg_prompt), construct_user("请根据之前的艺术讨论生成图片。"), construct_assistant(f"本张图片的 ImageID 是 {imageID}。\n\n{response}"))
    # write_json(data.userID, construct_prompt(pos_prompt + "\n" + neg_prompt), construct_user("Please generate an image based on our previous art discussion."), construct_assistant(f"ImageID is {imageID}.\n\n{response}"))
    print(data.history)
    return {"history": data.history, "image_url": image_url, "cnt": str(data.cnt), "imageID": imageID}

@dataclass
class ImageTopic:
    data: str = Form(...)
    image: UploadFile = Form(...)

@app.post("/image_edit_topic")  # 暂时不考虑user command只给评价和推荐
def gpt4_image_edit_topic(para: ImageTopic = Depends()):
    print(para.data)
    data = json.loads(para.data)
    image_bytes = para.image.file.read()
    image = Image.open(io.BytesIO(image_bytes))
    img = np.array(image)
    imageID = save_userID_image(data["userID"], image)

    # image_description = turbo_api(TRANSLATE, [construct_user(call_visualglm_api(img)["result"])])
    image_description = call_visualglm_api(img)["result"]

    res = gpt4_api(MODE_DECIDE, [construct_user(data["input"])])  # 输出01向量
    res_vec = extract_lists(res)  # 正则表达式提取出列表
    print(res_vec)
    res1 = filter_context(data["input"], res_vec)  # 输出有用的模态信息
    res2 = gpt4_api(EDIT_TOPIC_2, [construct_user(f"{res1},image:[{image_description}]")])  # 输出2个推荐主题
  
    vec_random = flip_random_bit(res_vec)  # 随机一个模态reverse
    res_random1 = filter_context(data["input"], vec_random)
    res_random2 = gpt4_api(EDIT_TOPIC_1, [construct_user(f"{res_random1},image:[{image_description}]")])
    # topic_output = construct_assistant("Received.\nYour userID is " + data["userID"] + f", imageID is {imageID}.\n\n" + image_description + "\n\n" + TOPIC_INTRO + res2 + "\n3. " + res_random2)
    topic_output = construct_assistant("收到图片。\n您的 userID 是 " + data["userID"] + f"，本张图片的 imageID 是 {imageID}。\n\n" + image_description + "\n\n" + EDIT_INTRO + res2 + "\n3. " + res_random2)
    data['history'].append(topic_output)
    write_json(data["userID"], construct_user(data["input"]), construct_vector(str(res_vec)), construct_context(res1), construct_vector(str(vec_random)), construct_vector(str(res_random1)), topic_output)
    
    print(data['history'])
    return {"history": data['history'], "imageID": imageID}
    

class ImageEditRequest(BaseModel):
    history: List[Dict[str, str]]
    userID: str
    editID: str
    
@app.post("/gpt4_sd_edit")
def gpt4_sd_edit(data: ImageEditRequest):  # 根据讨论修改图片
    tmp_history = data.history
    if len(data.history) > 0:  # 去掉绘画指令那一句
        data.history.pop()
    pos_prompt = gpt4_api(CN_TXT2IMG_PROMPT, data.history)
    print(f"pos_prompt: {pos_prompt}")
    data.history = tmp_history

    toolID = gpt4_api(EDIT_TOOLS, data.history)
    match = re.search(r'([1-5])', toolID)
    toolID = match.group(1)
    print(f"toolID:{toolID}")
    switch = {
        '1': lambda: controlnet_txt2img_api(f"output/{data.userID}/{data.editID}.jpg", pos_prompt, data.userID, "shuffle", "control_v11e_sd15_shuffle [526bfdae]"),  # 风格迁移
        '2': lambda: controlnet_txt2img_api(f"output/{data.userID}/{data.editID}.jpg", pos_prompt, data.userID, "softedge_hed", "control_v11p_sd15_lineart [43d4be0d]"),  # 风格化
        '3': lambda: controlnet_txt2img_api(f"output/{data.userID}/{data.editID}.jpg", pos_prompt, data.userID, "depth_zoe", "control_v11f1p_sd15_depth [cfd03158]"),  # 增减物体
        '4': lambda: controlnet_txt2img_api(f"output/{data.userID}/{data.editID}.jpg", pos_prompt, data.userID, "openpose_full", "control_v11p_sd15_openpose [cab727d4]"),  # 姿态控制
        '5': lambda: controlnet_txt2img_api(f"output/{data.userID}/{data.editID}.jpg", pos_prompt, data.userID, "mlsd", "control_v11p_sd15_mlsd [aca30ff0]")  # 建筑设计，适合建筑物和室内空间
    }
    func = switch.get(toolID)
    if func:
        new_images, imageID = func()
    else:
        print('无效的toolID')

    new_image = new_images[0]
    static_path = "static/images/" + str(uuid.uuid4()) + ".jpg"
    print("图片链接 http://166.111.139.116:22231/" + static_path)
    # print("图片链接 http://localhost:8000/" + static_path)
    new_image.save(static_path)
    # 构造URL
    image_url = "http://166.111.139.116:22231/" + static_path

    response = f"本张图片的 ImageID 是 {imageID}。\n\n" + call_visualglm_api(np.array(new_image))["result"]
    # response = f"ImageID is {imageID}.\n\n" + turbo_api(TRANSLATE, [construct_user(call_visualglm_api(np.array(new_image))["result"])])
    data.history.append(construct_assistant(response))
    write_json(data.userID, construct_prompt(pos_prompt), construct_assistant(f"toolID:{toolID}"), construct_assistant(response))
    print(data.history)
    return {"history": data.history, "image_url": image_url, "imageID": imageID}


@app.post("/gpt4_mode_1")  # 第一次实验
def gpt4_mode_1(data: ChatbotData):
    context_output = construct_user(data.input)

    res = gpt4_api(MODE_DECIDE, [context_output])  # 输出01向量
    res_vec = extract_lists(res)  # 正则表达式提取出列表
    vector_output = construct_vector(res)
    
    res1 = filter_context(data.input, res_vec)  # standard vector
    res2 = "您的 userID 是 " + data.userID + "。\n\n" + TOPIC_INTRO + "1." + gpt4_api(TOPIC_RECOMMEND_1, [construct_user(res1)]) + "\n"  # 输出1个推荐主题
    # res2 = "Your userID is " + data.userID + ".\n\n" + TOPIC_INTRO + "1." + gpt4_api(TOPIC_RECOMMEND_1, [construct_user(res1)]) + "\n"  # 输出1个推荐主题
    tmp = ""
    for i in range(len(res_vec)):  # 5个主题
        new_vector = res_vec.copy()
        new_vector[i] = 1 if new_vector[i] == 0 else 0
        res_context = filter_context(data.input, new_vector)
        tmp = tmp + res_context + "\n"
        res2 = res2 + str(i+2) + "." + gpt4_api(TOPIC_RECOMMEND_1, [construct_user(res_context)]) + "\n"  # 输出1个推荐主题
    
    topic_output = construct_assistant(res2)
    data.history.append(topic_output)
    write_json(data.userID, context_output, vector_output, construct_context(res1), construct_context(tmp), topic_output)

    print(data.history)
    return {"history": data.history}

@app.post("/gpt4_mode_2")  # 第二次实验（如果Phone Content很长，给出主题会损失一定信息，这时候用户会说出自己需求来纠正它）
def gpt4_mode_2(data: ChatbotData):
    res = gpt4_api(MODE_DECIDE, [construct_user(data.input)])  # 输出01向量
    res_vec = extract_lists(res)  # 正则表达式提取出列表
    print(res_vec)

    res1 = filter_context(data.input, res_vec)  # 输出有用的模态信息
    res2 = gpt4_api(TOPIC_RECOMMEND_2, [construct_user(res1)])  # 输出2个推荐主题
    print(res2)

    vec_random = flip_random_bit(res_vec)  # 随机一个模态reverse
    res_random1 = filter_context(data.input, vec_random)
    res_random2 = gpt4_api(TOPIC_RECOMMEND_1, [construct_user(res_random1)])
    topic_output = construct_assistant("您的 userID 是 " + data.userID + "。\n\n" + TOPIC_INTRO + res2 + "\n3. " + res_random2)
    # topic_output = construct_assistant("Your userID is " + data.userID + ".\n\n" + TOPIC_INTRO + res2 + "\n3. " + res_random2)
    data.history.append(topic_output)
    write_json(data.userID, construct_user(data.input), construct_vector(str(res_vec)), construct_context(res1), construct_vector(str(vec_random)), construct_vector(str(res_random1)), topic_output)

    print(data.history)
    return {"history": data.history}

@app.post("/gpt4_mode_3")  # 第三次实验
def gpt4_mode_3(data: ChatbotData):
    res = gpt4_api(MODE_DECIDE, [construct_user(data.input)])  # 输出01向量
    res_vec = extract_lists(res)  # 正则表达式提取出列表
    print(res_vec)

    res1 = filter_context(data.input, res_vec)  # 输出有用的模态信息
    res2 = gpt4_api(TOPIC_RECOMMEND_2, [construct_user(res1)])  # 输出2个推荐主题
    print(res2)

    vec_random = flip_random_bit(res_vec)  # 随机一个模态reverse
    res_random1 = filter_context(data.input, vec_random)
    res_random2 = gpt4_api(TOPIC_RECOMMEND_1, [construct_user(res_random1)])
    topic_output = construct_assistant("您的 userID 是 " + data.userID + "。\n\n" + TOPIC_INTRO + res2 + "\n3. " + res_random2)
    # topic_output = construct_assistant("Your userID is " + data.userID + ".\n\n" + TOPIC_INTRO + res2 + "\n3. " + res_random2)
    data.history.append(topic_output)
    write_json(data.userID, construct_user(data.input), construct_vector(str(res_vec)), construct_context(res1), construct_vector(str(vec_random)), construct_vector(str(res_random1)), topic_output)

    print(data.history)
    return {"history": data.history}


def construct_text(role, text):
    return {"role": role, "content": text}

def construct_user(text):
    return construct_text("user", text)

def construct_system(text):
    return construct_text("system", text)

def construct_assistant(text):
    return construct_text("assistant", text)

def construct_prompt(text):
    return construct_text("prompt", text)

def construct_photo(text):
    return construct_text("photo", text)

def construct_vector(text):
    return construct_text("vector", text)

def construct_context(text):
    return construct_text("context", text)


def gpt4_api(system, history):
    """ 返回str，参数为str,List """
    api_key = os.getenv('OPENAI_API_KEY')
    openai.api_key = api_key

    try:
        response = openai.ChatCompletion.create(model="gpt-4", messages=[construct_system(system), *history])
        return response['choices'][0]['message']['content']
    except openai.error.ServiceUnavailableError:
        print('The server is overloaded or not ready yet. Please try again later.')
        return None
    except Exception as e:
        print(f'Unexpected error occurred: {e}')
        return None


def turbo_api(system, history):
    """ 返回str，参数为str,List """
    api_key = os.getenv('OPENAI_API_KEY')
    openai.api_key = api_key

    try:
        response = openai.ChatCompletion.create(model="gpt-3.5-turbo-16k-0613", messages=[construct_system(system), *history])
        return response['choices'][0]['message']['content']
    except openai.error.ServiceUnavailableError:
        print('The server is overloaded or not ready yet. Please try again later.')
        return None
    except Exception as e:
        print(f'Unexpected error occurred: {e}')
        return None
    

def reset_user_input():
    return gr.update(value='')


def reset_state(chatbot, userID):
    chatbot.append((parse_text("A new painting theme."), parse_text("Alright, what kind of theme are you interested in creating?")))
    write_json(userID, construct_user("A new painting theme."), construct_assistant("Alright, what kind of theme are you interested in creating?"))
    yield chatbot, [], 0


def clear_gallery():
    return [], []


"""Override Chatbot.postprocess"""
def postprocess(self, y):
    if y is None:
        return []
    for i, (message, response) in enumerate(y):
        y[i] = (
            None if message is None else mdtex2html.convert((message)),
            None if response is None else mdtex2html.convert(response),
        )
    return y


def parse_text(text):  # 便于文本以html形式显示
    """copy from https://github.com/GaiZhenbiao/ChuanhuChatGPT/"""
    lines = text.split("\n")
    lines = [line for line in lines if line != ""]
    count = 0
    for i, line in enumerate(lines):
        if "```" in line:
            count += 1
            items = line.split('`')
            if count % 2 == 1:
                lines[i] = f'<pre><code class="language-{items[-1]}">'
            else:
                lines[i] = f'<br></code></pre>'
        else:
            if i > 0:
                if count % 2 == 1:
                    line = line.replace("`", "\`")
                    line = line.replace("<", "&lt;")
                    line = line.replace(">", "&gt;")
                    line = line.replace(" ", "&nbsp;")
                    line = line.replace("*", "&ast;")
                    line = line.replace("_", "&lowbar;")
                    line = line.replace("-", "&#45;")
                    line = line.replace(".", "&#46;")
                    line = line.replace("!", "&#33;")
                    line = line.replace("(", "&#40;")
                    line = line.replace(")", "&#41;")
                    line = line.replace("$", "&#36;")
                lines[i] = "<br>"+line
    text = "".join(lines)
    return text


def read_image(img, chatbot, history, userID):
    # 如果输入图像是PIL图像，将其转换为numpy数组
    if isinstance(img, Image.Image):
        img = np.array(img)
        
    process_and_save_image(img, userID)
    chatbot.append((parse_text("Please provide suggestions for this image."), ""))

    response0 = gpt4_api(TRANSLATE, [construct_user(call_visualglm_api(img))["result"]])
    response = gpt4_api(UPLOAD_ADVICE, [construct_user(response0)])

    chatbot[-1] = (parse_text("Please provide suggestions for this image."), parse_text(response)) 

    history.append(construct_user("Please provide suggestions for this image."))
    history.append(construct_assistant(response))
    write_json(userID, construct_user("Please provide suggestions for this image."), construct_assistant(response))
    print(history)
    yield chatbot, history

def process_and_save_image(np_image, userID):  # 存档用的，可以用于调取以往的数据！！！
    # 如果输入图像不是numpy数组，则进行转换
    if not isinstance(np_image, np.ndarray):
        np_image = np.array(np_image)
        
    # 确保我们有一个有效的numpy数组
    if np_image is None:
        raise ValueError("Image processing failed and resulted in None.")
    
    # 如果numpy数组不是uint8类型，则进行转换
    if np_image.dtype != np.uint8:
        np_image = np_image.astype(np.uint8)
        
    # 首先，确保numpy数组是uint8类型，且值在0-255范围内
    assert np_image.dtype == np.uint8
    assert np_image.min() >= 0
    assert np_image.max() <= 255
    
    # 将numpy数组转化为PIL图像
    img = Image.fromarray(np_image)
    
    # 将图像保存到指定路径
    img_path = 'output/' + time.strftime("%Y-%m-%d-%H-%M-%S-", time.localtime()) + str(random.randint(1000, 9999)) + "-upload-"  + userID + '.jpg'
    write_json(userID, construct_photo(img_path))
    img.save(img_path)
    img.save("output/edit-" + userID + ".jpg")


def encode_pil_to_base64(image):
    with io.BytesIO() as output_bytes:
        image.save(output_bytes, format="JPEG")
        bytes_data = output_bytes.getvalue()
    return base64.b64encode(bytes_data).decode("utf-8")

def controlnet_txt2img_api(image_path, pos_prompt, userID, cn_module, cn_model, sampler="DPM++ 2M Karras"):
    url = 'http://127.0.0.1:6016/sdapi/v1/txt2img'
    # url = "https://gt29495501.yicp.fun/sdapi/v1/txt2img"
    controlnet_image = Image.open(image_path)
    width, height = controlnet_image.size
    controlnet_image_data = encode_pil_to_base64(controlnet_image)
    txt2img_data = {
        "prompt": "((masterpiece, best quality, ultra-detailed, illustration))" + pos_prompt,
        "sampler_name": sampler,  # Euler也可
        "batch_size": 1,
        "step": 32,
        "cfg_scale": 7,
        "width": width,
        "height": height,
        "enabled": True,
        "negtive_prompt": "nsfw, (EasyNegative:0.8), (badhandv4:0.8), (missing fingers, multiple legs), (worst quality, low quality, extra digits, loli, loli face:1.2), lowres, blurry, text, logo, artist name, watermark",
        "alwayson_scripts": {
            "controlnet": {
                "args": [
                    {
                        "weight": 0.7,
                        "guidance start": 0.2,
                        "guidance end": 0.8,
                        "input_image": controlnet_image_data,
                        "module": cn_module,
                        "model": cn_model,
                        "pixel_perfect": True
                    }
                ]
            }
        }
    }
    response = requests.post(url, json=txt2img_data)
    print(txt2img_data["width"])
    print(txt2img_data["height"])
    r = response.json()
    image_list = []
    for i in r['images']:
        image = Image.open(io.BytesIO(base64.b64decode(i.split(",",1)[0])))
        image_list.append(image)
        # output_path = 'output/' + time.strftime("%Y-%m-%d-%H-%M-%S-", time.localtime()) + str(random.randint(1000, 9999)) + "-cn-"  + userID + '.jpg'
        imageID = save_userID_image(userID, image)
        write_json(userID, construct_photo(f"output/{userID}/{imageID}.jpg"))

    return image_list, imageID


def call_sd_t2i(userID, pos_prompt, neg_prompt, width, height):
    url = "http://127.0.0.1:6016"
    # url = "https://gt29495501.yicp.fun"
    payload = {
        "enable_hr": True,  # True画质更好但更慢
        # "enable_hr": False,  # True画质更好但更慢
        "denoising_strength": 0.55,
        "hr_scale": 1.5,
        "hr_upscaler": "Latent",
        "prompt": "((masterpiece, best quality, ultra-detailed, illustration))" + pos_prompt,
        "steps": 32,
        "negative_prompt": "nsfw, (EasyNegative:0.8), (badhandv4:0.8), (missing fingers, multiple legs, multiple hands), (worst quality, low quality, extra digits, loli, loli face:1.2), " + neg_prompt + ", lowres, blurry, text, logo, artist name, watermark",
        "cfg_scale": 7,
        "batch_size": 1,
        "n_iter": 1,
        "width": width,
        "height": height,
    }
    response = requests.post(url=f'{url}/sdapi/v1/txt2img', json=payload)
    r = response.json()
    image_list = []
    for i in r['images']:
        image = Image.open(io.BytesIO(base64.b64decode(i.split(",",1)[0])))
        image_list.append(image)
        # output_path = 'output/'+ time.strftime("%Y-%m-%d-%H-%M-%S-", time.localtime()) + str(user_input[:12]) + "-" + userID +'.jpg'
        imageID = save_userID_image(userID, image)
        write_json(userID, construct_photo(f"output/{userID}/{imageID}.jpg"))

    return image_list, imageID


def call_visualglm_api(img, history=[]):
    history = []  # 先不给历史
    prompt="详细描述这张图片，包括画中的人、景、物、构图、颜色等，不超过90字"
    url = "http://127.0.0.1:8080"

    # 将BGR图像转换为RGB图像
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img_byte = cv2.imencode('.jpg', img)[1]
    img_base64 = base64.b64encode(img_byte).decode("utf-8")
    payload = {
        "image": img_base64,
        "text": prompt,
        "history": history
    }
    response = requests.post(url, json=payload)
    response = response.json()
    return response